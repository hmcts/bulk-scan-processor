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
import static uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputDocumentType.FORM;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputDocumentType.OTHER;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputDocumentType.SSCS1;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputDocumentType.WILL;

@SuppressWarnings("checkstyle:LineLength")
public class SubtypeTest {

    private static final String SOME_SUBTYPE = "foo";

    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Test
    public void should_map_scannable_item_document_types_correctly() {
        asList(
            // null subtype
            new TestCase(new Given(SSCS1, null), new Then(DocumentType.OTHER, DocumentSubtype.SSCS1)),
            new TestCase(new Given(WILL, null), new Then(DocumentType.OTHER, DocumentSubtype.WILL)),
            new TestCase(new Given(COVERSHEET, null), new Then(DocumentType.COVERSHEET, null)),
            new TestCase(new Given(CHERISHED, null), new Then(DocumentType.CHERISHED, null)),
            new TestCase(new Given(OTHER, null), new Then(DocumentType.OTHER, null)),
            new TestCase(new Given(FORM, null), new Then(DocumentType.FORM, null)),
            // non-null subtype
            new TestCase(new Given(SSCS1, SOME_SUBTYPE), new Then(DocumentType.OTHER, DocumentSubtype.SSCS1)),
            new TestCase(new Given(WILL, SOME_SUBTYPE), new Then(DocumentType.OTHER, DocumentSubtype.WILL)),
            new TestCase(new Given(COVERSHEET, SOME_SUBTYPE), new Then(DocumentType.COVERSHEET, SOME_SUBTYPE)),
            new TestCase(new Given(CHERISHED, SOME_SUBTYPE), new Then(DocumentType.CHERISHED, SOME_SUBTYPE)),
            new TestCase(new Given(OTHER, SOME_SUBTYPE), new Then(DocumentType.OTHER, SOME_SUBTYPE)),
            new TestCase(new Given(FORM, SOME_SUBTYPE), new Then(DocumentType.FORM, SOME_SUBTYPE))
        ).forEach(tc -> {
            // given
            InputScannableItem item = inputScannableItem(tc.input.documentType, tc.input.docSubtype);

            // when
            ScannableItem result = EnvelopeMapper.toDbScannableItem(item);

            // then
            softly.assertThat(result.getDocumentType())
                .as("Output document type for type '%s' and subtype '%s'", tc.input.documentType, tc.input.docSubtype)
                .isEqualTo(tc.output.documentType);

            softly.assertThat(result.getDocumentSubtype())
                .as("Output document subtype for type '%s' and subtype '%s'", tc.input.documentType, tc.input.docSubtype)
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
        public String docSubtype;

        public Given(InputDocumentType documentType, String docSubtype) {
            this.documentType = documentType;
            this.docSubtype = docSubtype;
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
