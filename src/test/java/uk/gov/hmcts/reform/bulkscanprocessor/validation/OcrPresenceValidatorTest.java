package uk.gov.hmcts.reform.bulkscanprocessor.validation;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.OcrPresenceException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputDocumentType;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputOcrData;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputScannableItem;

import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputDocumentType.CHERISHED;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputDocumentType.FORM;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputDocumentType.OTHER;

@RunWith(MockitoJUnitRunner.class)
public class OcrPresenceValidatorTest {

    private final OcrPresenceValidator validator = new OcrPresenceValidator();

    @Test
    public void should_throw_exception_if_multiple_docs_contain_ocr() {
        assertThatThrownBy(
            () -> validator.assertHasProperlySetOcr(
                asList(
                    doc(FORM, new InputOcrData()),
                    doc(OTHER, new InputOcrData()),
                    doc(OTHER, null),
                    doc(CHERISHED, null)
                )
            ))
            .isInstanceOf(OcrPresenceException.class)
            .hasMessage(OcrPresenceValidator.MULTIPLE_OCR_MSG);
    }

    @Test
    public void should_throw_an_exception_when_form_has_no_ocr() {
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
    public void should_throw_an_exception_when_a_doc_that_is_not_form_has_ocr() {
        assertThatThrownBy(
            () -> validator.assertHasProperlySetOcr(
                asList(
                    doc(FORM, null),
                    doc(OTHER, new InputOcrData()),
                    doc(OTHER, null),
                    doc(CHERISHED, null)
                )
            ))
            .isInstanceOf(OcrPresenceException.class)
            .hasMessage(OcrPresenceValidator.MISPLACED_OCR_MSG);
    }

    @Test
    public void should_return_document_with_ocr() {
        // given
        InputScannableItem docWithOcr = doc(FORM, new InputOcrData());
        List<InputScannableItem> docs =
            asList(
                docWithOcr,
                doc(OTHER, null),
                doc(OTHER, null),
                doc(CHERISHED, null)
            );

        // when
        Optional<InputScannableItem> result = validator.assertHasProperlySetOcr(docs);

        // then
        assertThat(result).get().isEqualTo(docWithOcr);
    }

    @Test
    public void should_return_empty_optional_if_there_are_no_docs_with_ocr() {
        // given
        List<InputScannableItem> docs =
            asList(
                doc(OTHER, null),
                doc(OTHER, null),
                doc(CHERISHED, null)
            );

        // when
        Optional<InputScannableItem> result = validator.assertHasProperlySetOcr(docs);

        // then
        assertThat(result).isEmpty();
    }

    private InputScannableItem doc(InputDocumentType type, InputOcrData ocr) {
        return new InputScannableItem(
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
            null
        );
    }
}
