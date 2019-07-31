package uk.gov.hmcts.reform.bulkscanprocessor.validation;

import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.ImmutableList;
import io.github.netmikey.logunit.api.LogCapturer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.HttpClientErrorException.NotFound;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.bulkscanprocessor.config.ContainerMappings;
import uk.gov.hmcts.reform.bulkscanprocessor.config.ContainerMappings.Mapping;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.OcrValidationException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputEnvelope;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputOcrData;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputOcrDataField;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client.OcrValidationClient;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client.model.req.FormData;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client.model.res.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client.model.res.ValidationResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.validation.model.OcrValidationWarnings;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.InputEnvelopeCreator.inputEnvelope;

@ExtendWith(MockitoExtension.class)
public class OcrValidatorTest {

    private static final String VALIDATION_URL = "https://example.com/validate-ocr";
    private static final String S2S_TOKEN = "sample-s2s-token";
    private static final String PO_BOX = "sample PO box";

    @RegisterExtension
    public LogCapturer capturer = LogCapturer.create().captureForType(OcrValidator.class);

    @Mock private OcrValidationClient client;
    @Mock private ContainerMappings containerMappings;
    @Mock private AuthTokenGenerator authTokenGenerator;

    @Captor
    private ArgumentCaptor<FormData> argCaptor;

    private OcrValidator ocrValidator;

    @BeforeEach
    public void setUp() throws Exception {
        this.ocrValidator = new OcrValidator(client, containerMappings, authTokenGenerator);
    }

    @Test
    public void should_call_rest_client_with_correct_parameters() {
        // given
        String url = VALIDATION_URL;

        given(containerMappings.getMappings())
            .willReturn(singletonList(
                new Mapping("container", "jurisdiction", PO_BOX, url)
            ));

        given(client.validate(eq(url), any(), any(), any()))
            .willReturn(new ValidationResponse(Status.SUCCESS, emptyList(), emptyList()));

        given(authTokenGenerator.generate()).willReturn(S2S_TOKEN);

        // and
        String subtype = "sample_document_subtype";
        InputEnvelope envelope = inputEnvelope(
            "BULKSCAN",
            PO_BOX,
            Classification.EXCEPTION,
            asList(
                doc(subtype, sampleOcr()),
                doc("other", null)
            )
        );

        // when
        ocrValidator.assertOcrDataIsValid(envelope);

        // then
        verify(client).validate(eq(url), argCaptor.capture(), eq(subtype), eq(S2S_TOKEN));
        assertThat(argCaptor.getValue().ocrDataFields)
            .extracting(it -> tuple(it.name, it.value))
            .containsExactlyElementsOf(
                sampleOcr()
                    .getFields()
                    .stream()
                    .map(it -> tuple(it.name.textValue(), it.value.textValue()))
                    .collect(toList())
            );
    }

    @Test
    public void should_return_warnings_from_successful_validation_result() {
        // given
        given(containerMappings.getMappings())
            .willReturn(singletonList(
                new Mapping("container", "jurisdiction", PO_BOX, VALIDATION_URL)
            ));

        List<String> expectedWarnings = ImmutableList.of("warning 1", "warning 2");

        given(client.validate(any(), any(), any(), any()))
            .willReturn(new ValidationResponse(Status.SUCCESS, expectedWarnings, emptyList()));

        given(authTokenGenerator.generate()).willReturn(S2S_TOKEN);

        InputScannableItem scannableItem = doc("subtype1", sampleOcr());
        InputEnvelope envelope = envelope(PO_BOX, asList(scannableItem));

        // when
        Optional<OcrValidationWarnings> warnings = ocrValidator.assertOcrDataIsValid(envelope);

        // then
        assertThat(warnings).isPresent();
        assertThat(warnings.get().documentControlNumber).isEqualTo(scannableItem.documentControlNumber);
        assertThat(warnings.get().warnings).isEqualTo(expectedWarnings);
    }

    @Test
    public void should_handle_null_warnings_from_successful_validation_result() {
        // given
        given(containerMappings.getMappings())
            .willReturn(singletonList(
                new Mapping("container", "jurisdiction", PO_BOX, VALIDATION_URL)
            ));

        given(client.validate(any(), any(), any(), any()))
            .willReturn(new ValidationResponse(Status.SUCCESS, null, emptyList()));

        given(authTokenGenerator.generate()).willReturn(S2S_TOKEN);

        InputEnvelope envelope = envelope(PO_BOX, asList(doc("subtype1", sampleOcr())));

        // when
        Optional<OcrValidationWarnings> warnings = ocrValidator.assertOcrDataIsValid(envelope);

        // then
        assertThat(warnings).isPresent();
        assertThat(warnings.get().warnings).isEmpty();
    }

    @Test
    public void should_not_call_validation_if_url_is_not_configured() {
        // given
        InputEnvelope envelope = envelope(
            "samplePoBox",
            asList(
                doc("D8", sampleOcr()),
                doc("other", null)
            )
        );

        given(containerMappings.getMappings()).willReturn(emptyList()); // url not configured

        // when
        ocrValidator.assertOcrDataIsValid(envelope);

        // then
        verify(client, never()).validate(any(), any(), any(), any());
    }

    @Test
    public void should_not_call_validation_there_are_no_documents_with_ocr() {
        // given
        InputEnvelope envelope = envelope(
            PO_BOX,
            asList(
                doc("other", null),
                doc("other", null)
            )
        );

        given(containerMappings.getMappings())
            .willReturn(singletonList(
                new Mapping("c", "j", envelope.poBox, "https://example.com")
            ));

        // when
        ocrValidator.assertOcrDataIsValid(envelope);

        // then
        verify(client, never()).validate(any(), any(), any(), any());
    }

    @Test
    public void should_throw_an_exception_if_service_responded_with_error_response() {
        // given
        String subtype = "sample_document_subtype";
        InputEnvelope envelope = envelope(
            PO_BOX,
            asList(
                doc(subtype, sampleOcr()),
                doc("other", null)
            )
        );

        given(containerMappings.getMappings())
            .willReturn(singletonList(
                new Mapping("container", "jurisdiction", PO_BOX, VALIDATION_URL)
            ));

        given(client.validate(any(), any(), any(), any()))
            .willReturn(new ValidationResponse(Status.ERRORS, emptyList(), singletonList("Error!")));

        given(authTokenGenerator.generate()).willReturn(S2S_TOKEN);

        // when
        Throwable err = catchThrowable(() -> ocrValidator.assertOcrDataIsValid(envelope));


        // then
        assertThat(err)
            .isInstanceOf(OcrValidationException.class)
            .hasMessageContaining("Error!");
    }

    @Test
    public void should_throw_an_exception_if_service_responded_with_404() {
        // given
        String subtype = "sample_document_subtype";
        InputEnvelope envelope = envelope(
            PO_BOX,
            asList(
                doc(subtype, sampleOcr()),
                doc("other", null)
            )
        );

        given(containerMappings.getMappings())
            .willReturn(singletonList(
                new Mapping("container", "jurisdiction", PO_BOX, VALIDATION_URL)
            ));

        given(authTokenGenerator.generate()).willReturn(S2S_TOKEN);

        given(client.validate(any(), any(), any(), any()))
            .willThrow(NotFound.class);

        // when
        Throwable err = catchThrowable(() -> ocrValidator.assertOcrDataIsValid(envelope));


        // then
        assertThat(err)
            .isInstanceOf(OcrValidationException.class);
    }

    @Test
    public void should_continue_if_calling_validation_endpoint_fails() {
        // given
        InputScannableItem scannableItemWithOcr = doc("form", sampleOcr());
        InputEnvelope envelope = envelope(
            PO_BOX,
            asList(
                scannableItemWithOcr,
                doc("other", null)
            )
        );

        given(containerMappings.getMappings())
            .willReturn(singletonList(
                new Mapping("c", "j", envelope.poBox, VALIDATION_URL)
            ));

        given(client.validate(any(), any(), any(), any())).willThrow(new RuntimeException());

        given(authTokenGenerator.generate()).willReturn(S2S_TOKEN);

        // when
        Optional<OcrValidationWarnings> warnings = ocrValidator.assertOcrDataIsValid(envelope);

        // then
        assertThat(warnings).isPresent();
        assertThat(warnings.get().documentControlNumber).isEqualTo(scannableItemWithOcr.documentControlNumber);
        assertThat(warnings.get().warnings).containsExactly("OCR validation was not performed due to errors");
        verify(client).validate(any(), any(), any(), any());
    }

    @Test
    public void should_log_a_warning_if_more_than_one_doc_has_ocr() {
        // given
        InputEnvelope envelope = envelope(
            PO_BOX,
            asList(
                doc("form", sampleOcr()),
                doc("other", null),
                doc("other", sampleOcr()) // second document with OCR
            )
        );

        given(containerMappings.getMappings()).willReturn(emptyList());

        // when
        ocrValidator.assertOcrDataIsValid(envelope);

        // then
        capturer.assertContains("Multiple documents with OCR");
    }

    @Test
    public void should_log_info_when_ocr_is_present_but_theres_not_service_configured_to_validate_it() {
        // given
        InputEnvelope envelope = envelope(
            PO_BOX,
            asList(
                doc("form", sampleOcr()),
                doc("other", null)
            )
        );

        given(containerMappings.getMappings()).willReturn(emptyList());

        // when
        ocrValidator.assertOcrDataIsValid(envelope);

        // then
        capturer.assertContains("OCR validation URL for po box " + envelope.poBox + " not configured");
    }

    private InputOcrData sampleOcr() {
        InputOcrData data = new InputOcrData();
        data.setFields(asList(
            new InputOcrDataField(new TextNode("hello"), new TextNode("world")),
            new InputOcrDataField(new TextNode("foo"), new TextNode("bar"))
        ));
        return data;
    }

    private InputEnvelope envelope(String poBox, List<InputScannableItem> scannableItems) {
        return inputEnvelope(
            "BULKSCAN",
            poBox,
            Classification.EXCEPTION,
            scannableItems
        );
    }

    private InputScannableItem doc(String subtype, InputOcrData ocrData) {
        return new InputScannableItem(
            UUID.randomUUID().toString(),
            Instant.now(),
            null,
            null,
            null,
            null,
            ocrData,
            null,
            null,
            null,
            subtype
        );
    }
}
