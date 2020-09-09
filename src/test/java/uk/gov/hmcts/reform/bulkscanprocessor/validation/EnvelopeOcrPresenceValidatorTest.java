package uk.gov.hmcts.reform.bulkscanprocessor.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.OcrPresenceException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputDocumentType;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.DocumentType;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.OcrData;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.DocumentSubtype.SSCS1;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.DocumentType.CHERISHED;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.DocumentType.FORM;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.DocumentType.OTHER;

@ExtendWith(MockitoExtension.class)
class EnvelopeOcrPresenceValidatorTest {

    private final EnvelopeOcrPresenceValidator validator = new EnvelopeOcrPresenceValidator();

    @Test
    void should_throw_exception_if_multiple_docs_contain_ocr() {
        assertThatThrownBy(
            () -> validator.assertHasProperlySetOcr(
                asList(
                    doc(FORM, new OcrData(emptyList())),
                    doc(OTHER, new OcrData(emptyList())),
                    doc(OTHER, null),
                    doc(CHERISHED, null)
                )
            ))
            .isInstanceOf(OcrPresenceException.class)
            .hasMessage(OcrPresenceValidator.MULTIPLE_OCR_MSG);
    }

    @Test
    void should_throw_an_exception_when_form_has_no_ocr() {
        assertThatThrownBy(
            () -> validator.assertHasProperlySetOcr(
                asList(
                    doc(FORM, null),
                    doc(OTHER, null),
                    doc(OTHER, null),
                    doc(CHERISHED, null)
                )
            ))
            .isInstanceOf(OcrPresenceException.class)
            .hasMessage(OcrPresenceValidator.MISSING_OCR_MSG);
    }

    @Test
    void should_throw_an_exception_when_a_doc_that_is_not_form_or_sscs1_has_ocr() {
        EnumSet
            .allOf(InputDocumentType.class)
            .stream()
            .filter(docType -> !OcrPresenceValidator.OCR_DOC_TYPES.contains(docType))
            .forEach(invalidDocType -> {
                assertThatThrownBy(
                    () -> validator.assertHasProperlySetOcr(
                        asList(
                            doc(FORM, null),
                            doc(OTHER, new OcrData(emptyList())),
                            doc(OTHER, null),
                            doc(CHERISHED, null)
                        )
                    ))
                    .isInstanceOf(OcrPresenceException.class)
                    .hasMessage(OcrPresenceValidator.MISPLACED_OCR_MSG);
            });
    }

    @Test
    void should_throw_exception_doc_with_ocr_has_no_subtype() {
        assertThatThrownBy(
            () -> validator.assertHasProperlySetOcr(
                asList(
                    doc(FORM, null, new OcrData(emptyList())), // missing subtype
                    doc(OTHER, "some-subtype-1", null),
                    doc(CHERISHED, "some-subtype-2", null)
                )
            ))
            .isInstanceOf(OcrPresenceException.class)
            .hasMessage(OcrPresenceValidator.MISSING_DOC_SUBTYPE_MSG);
    }

    @Test
     void should_return_document_with_ocr_when_doctype_is_sscs1_and_subtype_is_not_set() {
        // given
        ScannableItem docWithOcr = doc(FORM, SSCS1, new OcrData(emptyList()));
        List<ScannableItem> docs =
            asList(
                docWithOcr,
                doc(OTHER, null),
                doc(OTHER, null),
                doc(CHERISHED, null)
            );

        // when
        Optional<ScannableItem> result = validator.assertHasProperlySetOcr(docs);

        // then
        assertThat(result).get().isEqualTo(docWithOcr);
    }

    @Test
     void should_return_document_with_ocr() {
        // given
        ScannableItem docWithOcr = doc(FORM, new OcrData(null));
        List<ScannableItem> docs =
            asList(
                docWithOcr,
                doc(OTHER, null),
                doc(OTHER, null),
                doc(CHERISHED, null)
            );

        // when
        Optional<ScannableItem> result = validator.assertHasProperlySetOcr(docs);

        // then
        assertThat(result).get().isEqualTo(docWithOcr);
    }

    @Test
    void should_return_empty_optional_if_there_are_no_docs_with_ocr() {
        // given
        List<ScannableItem> docs =
            asList(
                doc(OTHER, null),
                doc(OTHER, null),
                doc(CHERISHED, null)
            );

        // when
        Optional<ScannableItem> result = validator.assertHasProperlySetOcr(docs);

        // then
        assertThat(result).isEmpty();
    }

    private ScannableItem doc(DocumentType type, OcrData ocr) {
        return doc(type, "some-doc-subtype", ocr);
    }

    private ScannableItem doc(DocumentType type, String subtype, OcrData ocr) {
        return new ScannableItem(
            null,
            null,
            null,
            null,
            null,
            null,
            ocr,
            null,
            null,
            type,
            subtype,
            new String[0]
        );
    }

}
