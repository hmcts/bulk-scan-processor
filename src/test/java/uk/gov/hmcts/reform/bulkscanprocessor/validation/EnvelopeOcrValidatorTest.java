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
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.OcrValidationException;
import uk.gov.hmcts.reform.bulkscanprocessor.helper.EnvelopeCreator;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.DocumentSubtype;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.DocumentType;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.OcrData;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.OcrDataField;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification.EXCEPTION;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification.SUPPLEMENTARY_EVIDENCE;

@ExtendWith(MockitoExtension.class)
class EnvelopeOcrValidatorTest {

    private static final String VALIDATION_URL = "https://example.com/validate-ocr";
    private static final String S2S_TOKEN = "sample-s2s-token";
    private static final String PO_BOX = "sample PO box";

    @RegisterExtension
    LogCapturer capturer = LogCapturer.create().captureForType(EnvelopeOcrValidator.class);

    @Mock private OcrValidationClient client;
    @Mock private ContainerMappings containerMappings;
    @Mock private AuthTokenGenerator authTokenGenerator;

    @Captor
    private ArgumentCaptor<FormData> argCaptor;

    private EnvelopeOcrValidator ocrValidator;

    @BeforeEach
    void setUp() {
        this.ocrValidator = new EnvelopeOcrValidator(client, containerMappings, authTokenGenerator);
    }

    @Test
    void should_call_rest_client_with_correct_parameters() {
        // given
        String url = VALIDATION_URL;

        given(containerMappings.getMappings())
            .willReturn(singletonList(
                new Mapping("container", "jurisdiction", PO_BOX, url, true, true)
            ));

        given(client.validate(eq(url), any(), any(), any()))
            .willReturn(new ValidationResponse(Status.SUCCESS, emptyList(), emptyList()));

        given(authTokenGenerator.generate()).willReturn(S2S_TOKEN);

        // and
        ScannableItem docWithOcr = doc(DocumentType.FORM, "sample_document_subtype", sampleOcr());
        List<ScannableItem> docs =
            asList(
                docWithOcr,
                doc(DocumentType.OTHER, "other", null)
            );
        Envelope envelope = envelope(
            PO_BOX,
            docs,
            SUPPLEMENTARY_EVIDENCE
        );

        // when
        ocrValidator.assertOcrDataIsValid(envelope);

        // then
        verify(client).validate(eq(url), argCaptor.capture(), eq(docWithOcr.getDocumentSubtype()), eq(S2S_TOKEN));
        assertThat(argCaptor.getValue().ocrDataFields)
            .extracting(it -> tuple(it.name, it.value))
            .containsExactlyElementsOf(
                sampleOcr()
                    .fields
                    .stream()
                    .map(it -> tuple(it.name.textValue(), it.value.textValue()))
                    .collect(toList())
            );
    }

    @Test
    void should_handle_sscs1_forms_without_subtype() {
        // given
        String url = VALIDATION_URL;

        given(containerMappings.getMappings())
            .willReturn(singletonList(
                new Mapping("container", "jurisdiction", PO_BOX, url, true, true)
            ));

        given(client.validate(eq(url), any(), any(), any()))
            .willReturn(new ValidationResponse(Status.SUCCESS, emptyList(), emptyList()));

        given(authTokenGenerator.generate()).willReturn(S2S_TOKEN);

        // and
        ScannableItem docWithOcr = doc(DocumentType.FORM, DocumentSubtype.SSCS1, sampleOcr());
        List<ScannableItem> docs =
            asList(
                docWithOcr,
                doc(DocumentType.OTHER, "other", null)
            );
        Envelope envelope = envelope(
            PO_BOX,
            docs,
            SUPPLEMENTARY_EVIDENCE
        );

        // when
        ocrValidator.assertOcrDataIsValid(envelope);

        // then
        // an appropriate subtype is being used (instead of null)
        verify(client).validate(eq(url), any(), eq("sscs1"), eq(S2S_TOKEN));
    }

    @Test
    void should_not_call_rest_client_for_exception_journey_classification() {
        // given
        ScannableItem docWithOcr = doc(DocumentType.FORM, "sample_document_subtype", sampleOcr());
        List<ScannableItem> docs =
            asList(
                docWithOcr,
                doc(DocumentType.OTHER, "other", null)
            );
        Envelope envelope = envelope(
            PO_BOX,
            docs,
            EXCEPTION
        );

        // when
        Optional<OcrValidationWarnings> res = ocrValidator.assertOcrDataIsValid(envelope);

        // then
        verifyNoInteractions(client);
        assertThat(res.isPresent()).isFalse();
    }

    @Test
    void should_return_warnings_from_successful_validation_result() {
        // given
        given(containerMappings.getMappings())
            .willReturn(singletonList(
                new Mapping("container", "jurisdiction", PO_BOX, VALIDATION_URL, true, true)
            ));

        List<String> expectedWarnings = ImmutableList.of("warning 1", "warning 2");

        given(client.validate(any(), any(), any(), any()))
            .willReturn(new ValidationResponse(Status.SUCCESS, expectedWarnings, emptyList()));

        given(authTokenGenerator.generate()).willReturn(S2S_TOKEN);

        ScannableItem scannableItem = doc(DocumentType.FORM, "subtype1", sampleOcr());
        Envelope envelope = envelope(
            PO_BOX,
            singletonList(scannableItem),
            SUPPLEMENTARY_EVIDENCE
        );

        // when
        Optional<OcrValidationWarnings> warnings = ocrValidator.assertOcrDataIsValid(envelope);

        // then
        assertThat(warnings).isPresent();
        assertThat(warnings.get().documentControlNumber).isEqualTo(scannableItem.getDocumentControlNumber());
        assertThat(warnings.get().warnings).isEqualTo(expectedWarnings);
    }

    @Test
    void should_handle_null_warnings_from_successful_validation_result() {
        // given
        given(containerMappings.getMappings())
            .willReturn(singletonList(
                new Mapping("container", "jurisdiction", PO_BOX, VALIDATION_URL, true, true)
            ));

        given(client.validate(any(), any(), any(), any()))
            .willReturn(new ValidationResponse(Status.SUCCESS, null, emptyList()));

        given(authTokenGenerator.generate()).willReturn(S2S_TOKEN);

        Envelope envelope = envelope(
            PO_BOX,
            singletonList(doc(DocumentType.FORM, "z", sampleOcr())),
            SUPPLEMENTARY_EVIDENCE
        );

        // when
        Optional<OcrValidationWarnings> warnings = ocrValidator.assertOcrDataIsValid(envelope);

        // then
        assertThat(warnings).isPresent();
        assertThat(warnings.get().warnings).isEmpty();
    }

    @Test
    void should_not_call_validation_if_url_is_not_configured() {
        // given
        Envelope envelope = envelope(
            "samplePoBox",
            asList(
                doc(DocumentType.FORM, "D8", sampleOcr()),
                doc(DocumentType.OTHER, "other", null)
            ),
            SUPPLEMENTARY_EVIDENCE
        );

        given(containerMappings.getMappings()).willReturn(emptyList()); // url not configured

        // when
        ocrValidator.assertOcrDataIsValid(envelope);

        // then
        verify(client, never()).validate(any(), any(), any(), any());
    }

    @Test
    void should_not_call_validation_there_are_no_documents_with_ocr() {
        // given
        Envelope envelope = envelope(
            PO_BOX,
            asList(
                doc(DocumentType.OTHER, "other", null),
                doc(DocumentType.OTHER, "other", null)
            ),
            SUPPLEMENTARY_EVIDENCE
        );

        given(containerMappings.getMappings())
            .willReturn(singletonList(
                new Mapping("c", "j", envelope.getPoBox(), "https://example.com", true, true)
            ));

        // when
        ocrValidator.assertOcrDataIsValid(envelope);

        // then
        verify(client, never()).validate(any(), any(), any(), any());
    }

    @Test
    void should_throw_an_exception_if_service_responded_with_error_response() {
        // given
        Envelope envelope = envelope(
            PO_BOX,
            asList(
                doc(DocumentType.FORM, "y", sampleOcr()),
                doc(DocumentType.OTHER, "other", null)
            ),
            SUPPLEMENTARY_EVIDENCE
        );

        given(containerMappings.getMappings())
            .willReturn(singletonList(
                new Mapping("container", "jurisdiction", PO_BOX, VALIDATION_URL, true, true)
            ));

        given(client.validate(any(), any(), any(), any()))
            .willReturn(new ValidationResponse(Status.ERRORS, emptyList(), singletonList("Error!")));

        given(authTokenGenerator.generate()).willReturn(S2S_TOKEN);

        // when
        Throwable err = catchThrowable(() -> ocrValidator.assertOcrDataIsValid(envelope));

        // then
        assertThat(err)
            .isInstanceOf(OcrValidationException.class)
            .hasMessageContaining("OCR validation service returned OCR-specific errors");
    }

    @Test
    void should_throw_an_exception_if_service_responded_with_404() {
        // given
        Envelope envelope = envelope(
            PO_BOX,
            asList(
                doc(DocumentType.FORM, "x", sampleOcr()),
                doc(DocumentType.OTHER, "other", null)
            ),
            SUPPLEMENTARY_EVIDENCE
        );

        given(containerMappings.getMappings())
            .willReturn(singletonList(
                new Mapping("container", "jurisdiction", PO_BOX, VALIDATION_URL, true, true)
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
    void should_continue_if_calling_validation_endpoint_fails() {
        // given
        ScannableItem scannableItemWithOcr = doc(DocumentType.FORM, "form", sampleOcr());
        Envelope envelope = envelope(
            PO_BOX,
            asList(
                scannableItemWithOcr,
                doc(DocumentType.OTHER, "other", null)
            ),
            SUPPLEMENTARY_EVIDENCE
        );

        given(containerMappings.getMappings())
            .willReturn(singletonList(
                new Mapping("c", "j", envelope.getPoBox(), VALIDATION_URL, true, true)
            ));

        given(client.validate(any(), any(), any(), any()))
            .willThrow(new RuntimeException());

        given(authTokenGenerator.generate()).willReturn(S2S_TOKEN);

        // when
        Optional<OcrValidationWarnings> warnings = ocrValidator.assertOcrDataIsValid(envelope);

        // then
        assertThat(warnings).isPresent();
        assertThat(warnings.get().documentControlNumber).isEqualTo(scannableItemWithOcr.getDocumentControlNumber());
        assertThat(warnings.get().warnings).containsExactly("OCR validation was not performed due to errors");
        verify(client).validate(any(), any(), any(), any());
    }

    @Test
    void should_log_info_when_ocr_is_present_but_there_is_no_service_configured_to_validate_it() {
        // given
        Envelope envelope = envelope(
            PO_BOX,
            asList(
                doc(DocumentType.FORM, "form", sampleOcr()),
                doc(DocumentType.OTHER, "other", null)
            ),
            SUPPLEMENTARY_EVIDENCE
        );

        given(containerMappings.getMappings()).willReturn(emptyList());

        // when
        ocrValidator.assertOcrDataIsValid(envelope);

        // then
        capturer.assertContains("OCR validation URL for po box " + envelope.getPoBox() + " not configured");
    }

    private OcrData sampleOcr() {
        return new OcrData(
            asList(
                new OcrDataField(new TextNode("hello"), new TextNode("world")),
                new OcrDataField(new TextNode("foo"), new TextNode("bar"))
            )
        );
    }

    private Envelope envelope(
        String poBox,
        List<ScannableItem> scannableItems,
        Classification classification
    ) {
        return EnvelopeCreator.envelope(
            "BULKSCAN",
            poBox,
            classification,
            scannableItems
        );
    }

    private ScannableItem doc(DocumentType docType, String subtype, OcrData ocrData) {
        return new ScannableItem(
            UUID.randomUUID().toString(),
            Instant.now(),
            null,
            null,
            null,
            null,
            ocrData,
            null,
            null,
            docType,
            subtype,
            new String[0]
        );
    }
}
