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
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.EnvelopeCreator.inputScannableItem;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputDocumentType.CHERISHED;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputDocumentType.COVERSHEET;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputDocumentType.OTHER;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputDocumentType.SSCS1;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputDocumentType.WILL;

public class SubtypeTest {

    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Test
    public void should_map_scannable_item_document_types_correctly() {
        asList(
            new TestCase(new Given(SSCS1), new Then(DocumentType.OTHER, DocumentSubtype.SSCS1)),
            new TestCase(new Given(WILL), new Then(DocumentType.OTHER, DocumentSubtype.WILL)),
            new TestCase(new Given(COVERSHEET), new Then(DocumentType.OTHER, DocumentSubtype.COVERSHEET)),
            new TestCase(new Given(CHERISHED), new Then(DocumentType.CHERISHED, null)),
            new TestCase(new Given(OTHER), new Then(DocumentType.OTHER, null))
        ).forEach(tc -> {
            // given
            InputScannableItem item = inputScannableItem(tc.input.documentType);

            // when
            ScannableItem result = EnvelopeMapper.toDbScannableItem(item);

            // then
            softly.assertThat(result.getDocumentType())
                .as("Output document type for type '%s'", tc.input.documentType)
                .isEqualTo(tc.output.documentType);

            softly.assertThat(result.getDocumentSubtype())
                .as("Output document subtype for type '%s'", tc.input.documentType)
                .isEqualTo(tc.output.docSubtype);
        });
    }

    // region helper/dsl classes
    private class TestCase {
        public Given input;
        public Then output;

        // region constructor
        public TestCase(Given input, Then output) {
            this.input = input;
            this.output = output;
        }
        // endregion
    }

    private class Given {
        public InputDocumentType documentType;

        public Given(InputDocumentType documentType) {
            this.documentType = documentType;
        }
    }

    private class Then {
        public DocumentType documentType;
        public String docSubtype;

        public Then(DocumentType documentType, String docSubtype) {
            this.documentType = documentType;
            this.docSubtype = docSubtype;
        }
    }
    // endregion
}
