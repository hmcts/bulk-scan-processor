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
            new TestCase(new Given(SSCS1), new Expect(DocumentType.OTHER, DocumentSubtype.SSCS1)),
            new TestCase(new Given(WILL), new Expect(DocumentType.OTHER, DocumentSubtype.WILL)),
            new TestCase(new Given(COVERSHEET), new Expect(DocumentType.OTHER, DocumentSubtype.COVERSHEET)),
            new TestCase(new Given(CHERISHED), new Expect(DocumentType.CHERISHED, null)),
            new TestCase(new Given(OTHER), new Expect(DocumentType.OTHER, null))
        ).forEach(tc -> {
            // given
            InputScannableItem item = inputScannableItem(tc.input.documentType);

            // when
            ScannableItem result = EnvelopeMapper.toDbScannableItem(item);

            // then
            softly.assertThat(result.getDocumentType())
                .as("Output document type for type '%s'", tc.input.documentType)
                .isEqualTo(tc.expected.documentType);

            softly.assertThat(result.getDocumentSubtype())
                .as("Output document subtype for type '%s'", tc.input.documentType)
                .isEqualTo(tc.expected.docSubtype);
        });
    }

    // region helper/dsl classes
    private class TestCase {
        public Given input;
        public Expect expected;

        // region constructor
        public TestCase(Given input, Expect expected) {
            this.input = input;
            this.expected = expected;
        }
        // endregion
    }

    private class Given {
        public InputDocumentType documentType;

        public Given(InputDocumentType documentType) {
            this.documentType = documentType;
        }
    }

    private class Expect {
        public DocumentType documentType;
        public String docSubtype;

        public Expect(DocumentType documentType, String docSubtype) {
            this.documentType = documentType;
            this.docSubtype = docSubtype;
        }
    }
    // endregion
}
