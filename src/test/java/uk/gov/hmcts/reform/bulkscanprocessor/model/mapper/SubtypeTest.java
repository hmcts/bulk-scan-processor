package uk.gov.hmcts.reform.bulkscanprocessor.model.mapper;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputDocumentType;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.DocumentSubtype;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.DocumentType;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.EnvelopeCreator.inputScannableItem;

public class SubtypeTest {

    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Test
    public void should_map_scannable_item_document_types_correctly() {
        asList(
            new TestCase(inputScannableItem(InputDocumentType.SSCS1), DocumentType.OTHER, DocumentSubtype.SSCS1),
            new TestCase(inputScannableItem(InputDocumentType.WILL), DocumentType.OTHER, DocumentSubtype.WILL),
            new TestCase(inputScannableItem(InputDocumentType.COVERSHEET),
                DocumentType.OTHER,
                DocumentSubtype.COVERSHEET
            ),
            new TestCase(inputScannableItem(InputDocumentType.CHERISHED),
                DocumentType.CHERISHED, null),
            new TestCase(inputScannableItem(InputDocumentType.OTHER), DocumentType.OTHER, null)
        ).forEach(testCase -> {
            // given
            InputScannableItem item = testCase.input;

            // when
            ScannableItem result = EnvelopeMapper.toDbScannableItem(item);

            // then
            assertThat(result.getDocumentType()).isEqualTo(testCase.expectedDocType);
            assertThat(result.getDocumentSubtype()).isEqualTo(testCase.expectedDocSubtype);
        });
    }

    private class TestCase {
        InputScannableItem input;
        DocumentType expectedDocType;
        String expectedDocSubtype;

        // region constructor
        TestCase(InputScannableItem input, DocumentType expectedDocType, String expectedDocSubtype) {
            this.input = input;
            this.expectedDocType = expectedDocType;
            this.expectedDocSubtype = expectedDocSubtype;
        }
        // endregion
    }
}
