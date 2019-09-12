package uk.gov.hmcts.reform.bulkscanprocessor.validation.envelope;

import org.junit.Test;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.DisallowedDocumentTypesException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputDocumentType;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputEnvelope;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification;
import uk.gov.hmcts.reform.bulkscanprocessor.validation.EnvelopeValidator;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowable;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.InputEnvelopeCreator.inputEnvelope;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.InputEnvelopeCreator.scannableItem;

public class DocumentTypesValidationTest {

    @Test
    public void should_throw_exception_when_supplementary_evidence_envelope_contains_form() {
        // given
        InputEnvelope envelope = envelope(
            Classification.SUPPLEMENTARY_EVIDENCE,
            asList(
                scannableItem("doc1.pdf", InputDocumentType.OTHER),
                scannableItem("doc2.pdf", InputDocumentType.FORM)
            )
        );

        // when
        Throwable throwable = catchThrowable(
            () -> EnvelopeValidator.assertEnvelopeContainsDocsOfAllowedTypesOnly(envelope)
        );

        // then
        assertThat(throwable)
            .isInstanceOf(DisallowedDocumentTypesException.class)
            .hasMessage(
                "Envelope contains scannable item(s) of types that are not allowed for "
                    + "classification 'supplementary_evidence': [Form]"
            );
    }

    @Test
    public void should_throw_exception_when_supplementary_evidence_envelope_contains_sscs1_doc() {
        // given
        InputEnvelope envelope = envelope(
            Classification.SUPPLEMENTARY_EVIDENCE,
            asList(
                scannableItem("doc1.pdf", InputDocumentType.SSCS1),
                scannableItem("doc2.pdf", InputDocumentType.CHERISHED)
            )
        );

        // when
        Throwable throwable = catchThrowable(
            () -> EnvelopeValidator.assertEnvelopeContainsDocsOfAllowedTypesOnly(envelope)
        );

        // then
        assertThat(throwable)
            .isInstanceOf(DisallowedDocumentTypesException.class)
            .hasMessage(
                "Envelope contains scannable item(s) of types that are not allowed for "
                    + "classification 'supplementary_evidence': [SSCS1]"
            );
    }

    @Test
    public void should_not_throw_exception_when_form_or_sscs1_doc_found_for_new_application() {
        // given
        InputEnvelope envelope = envelope(
            Classification.NEW_APPLICATION,
            asList(
                scannableItem("doc1.pdf", InputDocumentType.SSCS1),
                scannableItem("doc2.pdf", InputDocumentType.FORM)
            )
        );

        // when
        assertThatCode(
            () -> EnvelopeValidator.assertEnvelopeContainsDocsOfAllowedTypesOnly(envelope)
        ).doesNotThrowAnyException();
    }

    @Test
    public void should_not_throw_exception_when_form_or_sscs1_found_for_exception() {
        // given
        InputEnvelope envelope = envelope(
            Classification.EXCEPTION,
            asList(
                scannableItem("doc1.pdf", InputDocumentType.SSCS1),
                scannableItem("doc2.pdf", InputDocumentType.FORM)
            )
        );

        // when
        assertThatCode(
            () -> EnvelopeValidator.assertEnvelopeContainsDocsOfAllowedTypesOnly(envelope)
        ).doesNotThrowAnyException();
    }

    @Test
    public void should_not_throw_exception_when_forms_or_sscs1_docs_are_absent_for_supplementary_evidence() {
        // given
        InputEnvelope envelope = envelope(
            Classification.SUPPLEMENTARY_EVIDENCE,
            asList(
                scannableItem("doc1.pdf", InputDocumentType.CHERISHED),
                scannableItem("doc2.pdf", InputDocumentType.OTHER)
            )
        );

        // when
        assertThatCode(
            () -> EnvelopeValidator.assertEnvelopeContainsDocsOfAllowedTypesOnly(envelope)
        ).doesNotThrowAnyException();
    }

    private InputEnvelope envelope(Classification classification, List<InputScannableItem> scannableItems) {
        return inputEnvelope(
            "BULKSCAN",
            "poBox",
            classification,
            scannableItems,
            emptyList()
        );
    }
}
