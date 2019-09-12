package uk.gov.hmcts.reform.bulkscanprocessor.validation;

import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.ImmutableList;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.web.client.HttpClientErrorException.NotFound;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.bulkscanprocessor.config.ContainerMappings;
import uk.gov.hmcts.reform.bulkscanprocessor.config.ContainerMappings.Mapping;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.OcrPresenceException;
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

@RunWith(MockitoJUnitRunner.class)
public class OcrValidatorTest {

    private static final String VALIDATION_URL = "https://example.com/validate-ocr";
    private static final String S2S_TOKEN = "sample-s2s-token";
    private static final String PO_BOX = "sample PO box";

    @Rule
    public OutputCapture outputCapture = new OutputCapture();

    @Mock private OcrValidationClient client;
    @Mock private OcrPresenceValidator presenceValidator;
    @Mock private ContainerMappings containerMappings;
    @Mock private AuthTokenGenerator authTokenGenerator;

    @Captor
    private ArgumentCaptor<FormData> argCaptor;

    private OcrValidator ocrValidator;

    @Before
    public void setUp() throws Exception {
        this.ocrValidator = new OcrValidator(client, presenceValidator, containerMappings, authTokenGenerator);
    }

    @After
    public void tearDown() throws Exception {
        outputCapture.flush();
    }

    @Test
    public void should_validate_the_presence_of_ocr_data() {
        // given
        List<InputScannableItem> docs =
            asList(
                doc("other", sampleOcr()),
                doc("other", null)
            );
        InputEnvelope envelope =
            inputEnvelope(
                "BULKSCAN",
                PO_BOX,
                Classification.EXCEPTION,
                docs
            );
        given(presenceValidator.assertHasProperlySetOcr(docs))
            .willThrow(new OcrPresenceException("msg"));

        // when
        Throwable exc = catchThrowable(() -> ocrValidator.assertOcrDataIsValid(envelope));

        // then
        assertThat(exc)
            .isInstanceOf(OcrPresenceException.class)
            .hasMessage("msg");
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
        InputScannableItem docWithOcr = doc("sample_document_subtype", sampleOcr());
        List<InputScannableItem> docs =
            asList(
                docWithOcr,
                doc("other", null)
            );
        InputEnvelope envelope = inputEnvelope(
            "BULKSCAN",
            PO_BOX,
            Classification.EXCEPTION,
            docs
        );
        // and
        given(presenceValidator.assertHasProperlySetOcr(envelope.scannableItems))
            .willReturn(Optional.of(docWithOcr));

        // when
        ocrValidator.assertOcrDataIsValid(envelope);

        // then
        verify(client).validate(eq(url), argCaptor.capture(), eq(docWithOcr.documentSubtype), eq(S2S_TOKEN));
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

        given(presenceValidator.assertHasProperlySetOcr(envelope.scannableItems))
            .willReturn(Optional.of(scannableItem));

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

        InputEnvelope envelope = envelope(PO_BOX, asList(doc("z", sampleOcr())));

        given(presenceValidator.assertHasProperlySetOcr(envelope.scannableItems))
            .willReturn(Optional.of(doc("z", sampleOcr())));

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
        InputEnvelope envelope = envelope(
            PO_BOX,
            asList(
                doc("y", sampleOcr()),
                doc("other", null)
            )
        );

        given(presenceValidator.assertHasProperlySetOcr(any()))
            .willReturn(Optional.of(doc("y", sampleOcr())));

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
        InputEnvelope envelope = envelope(
            PO_BOX,
            asList(
                doc("x", sampleOcr()),
                doc("other", null)
            )
        );

        given(presenceValidator.assertHasProperlySetOcr(any()))
            .willReturn(Optional.of(doc("x", sampleOcr())));

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

        given(presenceValidator.assertHasProperlySetOcr(envelope.scannableItems))
            .willReturn(Optional.of(scannableItemWithOcr));

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
        assertThat(outputCapture.toString())
            .contains("OCR validation URL for po box " + envelope.poBox + " not configured");
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
