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
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.OcrPresenceException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.OcrValidationException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputDocumentType;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.InputEnvelopeCreator.inputEnvelope;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputDocumentType.FORM;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputDocumentType.OTHER;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputDocumentType.SSCS1;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification.EXCEPTION;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification.SUPPLEMENTARY_EVIDENCE;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification.SUPPLEMENTARY_EVIDENCE_WITH_OCR;

@ExtendWith(MockitoExtension.class)
class OcrValidatorTest {

    private static final String VALIDATION_URL = "https://example.com/validate-ocr";
    private static final String S2S_TOKEN = "sample-s2s-token";
    private static final String PO_BOX_1 = "sample PO box 1";
    private static final String PO_BOX_2 = "sample PO box 2";

    @RegisterExtension
    public LogCapturer capturer = LogCapturer.create().captureForType(OcrValidator.class);

    @Mock private OcrValidationClient client;
    @Mock private OcrPresenceValidator presenceValidator;
    @Mock private ContainerMappings containerMappings;
    @Mock private AuthTokenGenerator authTokenGenerator;

    @Captor
    private ArgumentCaptor<FormData> argCaptor;

    private OcrValidator ocrValidator;

    @BeforeEach
    void setUp() throws Exception {
        this.ocrValidator = new OcrValidator(client, presenceValidator, containerMappings, authTokenGenerator);
    }

    @Test
    void should_validate_the_presence_of_ocr_data() {
        // given
        List<InputScannableItem> docs =
            asList(
                doc(OTHER, "other", sampleOcr()),
                doc(OTHER, "other", null)
            );
        InputEnvelope envelope =
            inputEnvelope(
                "BULKSCAN",
                PO_BOX_1,
                SUPPLEMENTARY_EVIDENCE_WITH_OCR,
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
    void should_call_rest_client_with_correct_parameters() {
        // given
        String url = VALIDATION_URL;

        given(containerMappings.getMappings())
            .willReturn(singletonList(
                new Mapping("container", "jurisdiction", singletonList(PO_BOX_1), url, true, true)
            ));

        given(client.validate(eq(url), any(), any(), any()))
            .willReturn(new ValidationResponse(Status.SUCCESS, emptyList(), emptyList()));

        given(authTokenGenerator.generate()).willReturn(S2S_TOKEN);

        // and
        InputScannableItem docWithOcr = doc(FORM, "sample_document_subtype", sampleOcr());
        List<InputScannableItem> docs =
            asList(
                docWithOcr,
                doc(OTHER, "other", null)
            );
        InputEnvelope envelope = inputEnvelope(
            "BULKSCAN",
            PO_BOX_1,
            SUPPLEMENTARY_EVIDENCE,
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
    void should_call_rest_client_with_correct_parameters_when_multiple_po_boxes() {
        // given
        String url = VALIDATION_URL;

        given(containerMappings.getMappings())
            .willReturn(singletonList(
                new Mapping("container", "jurisdiction", asList(PO_BOX_1, PO_BOX_2), url, true, true)
            ));

        given(client.validate(eq(url), any(), any(), any()))
            .willReturn(new ValidationResponse(Status.SUCCESS, emptyList(), emptyList()));

        given(authTokenGenerator.generate()).willReturn(S2S_TOKEN);

        // and
        InputScannableItem docWithOcr = doc(FORM, "sample_document_subtype", sampleOcr());
        List<InputScannableItem> docs =
            asList(
                docWithOcr,
                doc(OTHER, "other", null)
            );
        InputEnvelope envelope = inputEnvelope(
            "BULKSCAN",
            PO_BOX_2,
            SUPPLEMENTARY_EVIDENCE,
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
    void should_handle_sscs1_forms_without_subtype() {
        // given
        String url = VALIDATION_URL;

        given(containerMappings.getMappings())
            .willReturn(singletonList(
                new Mapping("container", "jurisdiction", singletonList(PO_BOX_1), url, true, true)
            ));

        given(client.validate(eq(url), any(), any(), any()))
            .willReturn(new ValidationResponse(Status.SUCCESS, emptyList(), emptyList()));

        given(authTokenGenerator.generate()).willReturn(S2S_TOKEN);

        // and
        InputScannableItem docWithOcr = doc(SSCS1, null, sampleOcr());
        List<InputScannableItem> docs =
            asList(
                docWithOcr,
                doc(OTHER, "other", null)
            );
        InputEnvelope envelope = inputEnvelope(
            "BULKSCAN",
             PO_BOX_1,
            SUPPLEMENTARY_EVIDENCE,
            docs
        );
        // and
        given(presenceValidator.assertHasProperlySetOcr(envelope.scannableItems))
            .willReturn(Optional.of(docWithOcr));

        // when
        ocrValidator.assertOcrDataIsValid(envelope);

        // then
        // an appropriate subtype is being used (instead of null)
        verify(client).validate(eq(url), any(), eq("SSCS1"), eq(S2S_TOKEN));
    }

    @Test
    void should_not_call_rest_client_for_exception_journey_classification() {
        // given
        InputScannableItem docWithOcr = doc(FORM, "sample_document_subtype", sampleOcr());
        List<InputScannableItem> docs =
            asList(
                docWithOcr,
                doc(OTHER, "other", null)
            );
        InputEnvelope envelope = inputEnvelope(
            "BULKSCAN",
            PO_BOX_1,
            EXCEPTION,
            docs
        );

        // when
        Optional<OcrValidationWarnings> res = ocrValidator.assertOcrDataIsValid(envelope);

        // then
        verifyNoInteractions(client);
        assertThat(res).isNotPresent();
    }

    @Test
    void should_return_warnings_from_successful_validation_result() {
        // given
        given(containerMappings.getMappings())
            .willReturn(singletonList(
                new Mapping("container", "jurisdiction", singletonList(PO_BOX_1), VALIDATION_URL, true, true)
            ));

        List<String> expectedWarnings = ImmutableList.of("warning 1", "warning 2");

        given(client.validate(any(), any(), any(), any()))
            .willReturn(new ValidationResponse(Status.SUCCESS, expectedWarnings, emptyList()));

        given(authTokenGenerator.generate()).willReturn(S2S_TOKEN);

        InputScannableItem scannableItem = doc(FORM, "subtype1", sampleOcr());
        InputEnvelope envelope = envelope(
            PO_BOX_1,
            singletonList(scannableItem),
            SUPPLEMENTARY_EVIDENCE
        );

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
    void should_handle_null_warnings_from_successful_validation_result() {
        // given
        given(containerMappings.getMappings())
            .willReturn(singletonList(
                new Mapping("container", "jurisdiction", singletonList(PO_BOX_1), VALIDATION_URL, true, true)
            ));

        given(client.validate(any(), any(), any(), any()))
            .willReturn(new ValidationResponse(Status.SUCCESS, null, emptyList()));

        given(authTokenGenerator.generate()).willReturn(S2S_TOKEN);

        InputEnvelope envelope = envelope(
            PO_BOX_1,
            singletonList(doc(FORM, "z", sampleOcr())),
            SUPPLEMENTARY_EVIDENCE
        );

        given(presenceValidator.assertHasProperlySetOcr(envelope.scannableItems))
            .willReturn(Optional.of(doc(FORM, "z", sampleOcr())));

        // when
        Optional<OcrValidationWarnings> warnings = ocrValidator.assertOcrDataIsValid(envelope);

        // then
        assertThat(warnings).isPresent();
        assertThat(warnings.get().warnings).isEmpty();
    }

    @Test
    void should_not_call_validation_if_url_is_not_configured() {
        // given
        InputEnvelope envelope = envelope(
            "samplePoBox",
            asList(
                doc(FORM, "D8", sampleOcr()),
                doc(OTHER, "other", null)
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
    void should_not_call_validation_if_url_is_not_configured_for_po_box() {
        // given
        given(containerMappings.getMappings())
                .willReturn(singletonList(
                        new Mapping("container", "jurisdiction", singletonList(PO_BOX_1), VALIDATION_URL, true, true)
                ));

        InputEnvelope envelope = envelope(
            PO_BOX_2,
            asList(
                doc(FORM, "D8", sampleOcr()),
                doc(OTHER, "other", null)
            ),
            SUPPLEMENTARY_EVIDENCE
        );

        // when
        ocrValidator.assertOcrDataIsValid(envelope);

        // then
        verify(client, never()).validate(any(), any(), any(), any());
    }

    @Test
    void should_not_call_validation_there_are_no_documents_with_ocr() {
        // given
        InputEnvelope envelope = envelope(
            PO_BOX_1,
            asList(
                doc(OTHER, "other", null),
                doc(OTHER, "other", null)
            ),
            SUPPLEMENTARY_EVIDENCE
        );

        given(containerMappings.getMappings())
            .willReturn(singletonList(
                new Mapping("c", "j", singletonList(envelope.poBox), "https://example.com", true, true)
            ));

        // when
        ocrValidator.assertOcrDataIsValid(envelope);

        // then
        verify(client, never()).validate(any(), any(), any(), any());
    }

    @Test
    void should_throw_an_exception_if_service_responded_with_error_response() {
        // given
        InputEnvelope envelope = envelope(
            PO_BOX_1,
            asList(
                doc(FORM, "y", sampleOcr()),
                doc(OTHER, "other", null)
            ),
            SUPPLEMENTARY_EVIDENCE
        );

        given(presenceValidator.assertHasProperlySetOcr(any()))
            .willReturn(Optional.of(doc(FORM, "y", sampleOcr())));

        given(containerMappings.getMappings())
            .willReturn(singletonList(
                new Mapping("container", "jurisdiction", singletonList(PO_BOX_1), VALIDATION_URL, true, true)
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
    void should_handle_null_reponse_code() {
        // given
        given(containerMappings.getMappings())
            .willReturn(singletonList(
                new Mapping("container", "jurisdiction", singletonList(PO_BOX_1), VALIDATION_URL, true, true)
            ));

        given(client.validate(any(), any(), any(), any()))
            .willReturn(new ValidationResponse(null, null, emptyList()));

        given(authTokenGenerator.generate()).willReturn(S2S_TOKEN);

        InputEnvelope envelope = envelope(
            PO_BOX_1,
            singletonList(doc(FORM, "z", sampleOcr())),
            SUPPLEMENTARY_EVIDENCE
        );

        given(presenceValidator.assertHasProperlySetOcr(envelope.scannableItems))
            .willReturn(Optional.of(doc(FORM, "z", sampleOcr())));

        // when
        Optional<OcrValidationWarnings> warnings = ocrValidator.assertOcrDataIsValid(envelope);

        // then
        assertThat(warnings).isPresent();
        assertThat(warnings.get().warnings).isEmpty();
    }

    @Test
    void should_throw_an_exception_if_service_responded_with_404() {
        // given
        InputEnvelope envelope = envelope(
            PO_BOX_1,
            asList(
                doc(FORM, "x", sampleOcr()),
                doc(OTHER, "other", null)
            ),
            SUPPLEMENTARY_EVIDENCE
        );

        given(presenceValidator.assertHasProperlySetOcr(any()))
            .willReturn(Optional.of(doc(FORM, "x", sampleOcr())));

        given(containerMappings.getMappings())
            .willReturn(singletonList(
                new Mapping("container", "jurisdiction", singletonList(PO_BOX_1), VALIDATION_URL, true, true)
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
        InputScannableItem scannableItemWithOcr = doc(FORM, "form", sampleOcr());
        InputEnvelope envelope = envelope(
            PO_BOX_1,
            asList(
                scannableItemWithOcr,
                doc(OTHER, "other", null)
            ),
            SUPPLEMENTARY_EVIDENCE
        );

        given(presenceValidator.assertHasProperlySetOcr(envelope.scannableItems))
            .willReturn(Optional.of(scannableItemWithOcr));

        given(containerMappings.getMappings())
            .willReturn(singletonList(
                new Mapping("c", "j", singletonList(envelope.poBox), VALIDATION_URL, true, true)
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
    void should_log_info_when_ocr_is_present_but_theres_not_service_configured_to_validate_it() {
        // given
        InputEnvelope envelope = envelope(
            PO_BOX_1,
            asList(
                doc(FORM, "form", sampleOcr()),
                doc(OTHER, "other", null)
            ),
            SUPPLEMENTARY_EVIDENCE
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

    private InputEnvelope envelope(
        String poBox,
        List<InputScannableItem> scannableItems,
        Classification classification
    ) {
        return inputEnvelope(
            "BULKSCAN",
            poBox,
            classification,
            scannableItems
        );
    }

    private InputScannableItem doc(InputDocumentType docType, String subtype, InputOcrData ocrData) {
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
            docType,
            subtype
        );
    }
}
