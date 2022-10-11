package uk.gov.hmcts.reform.bulkscanprocessor.model.mapper;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
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
import static uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputDocumentType.SUPPORTING_DOCUMENTS;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputDocumentType.WILL;

class SubtypeTest {

    private static final String SOME_SUBTYPE = "foo";

    @Test
    void should_map_scannable_item_document_types_correctly() {
        asList(
            // null subtype
            new TestCase(new Given(SSCS1, null), new Expect(DocumentType.FORM, DocumentSubtype.SSCS1)),
            new TestCase(new Given(WILL, null), new Expect(DocumentType.WILL, null)),
            new TestCase(new Given(COVERSHEET, null), new Expect(DocumentType.COVERSHEET, null)),
            new TestCase(new Given(CHERISHED, null), new Expect(DocumentType.CHERISHED, null)),
            new TestCase(new Given(OTHER, null), new Expect(DocumentType.OTHER, null)),
            new TestCase(new Given(FORM, null), new Expect(DocumentType.FORM, null)),
            new TestCase(new Given(SUPPORTING_DOCUMENTS, null), new Expect(DocumentType.SUPPORTING_DOCUMENTS, null)),
            // non-null subtype
            new TestCase(new Given(SSCS1, SOME_SUBTYPE), new Expect(DocumentType.FORM, DocumentSubtype.SSCS1)),
            new TestCase(new Given(WILL, SOME_SUBTYPE), new Expect(DocumentType.WILL, SOME_SUBTYPE)),
            new TestCase(new Given(COVERSHEET, SOME_SUBTYPE), new Expect(DocumentType.COVERSHEET, SOME_SUBTYPE)),
            new TestCase(new Given(CHERISHED, SOME_SUBTYPE), new Expect(DocumentType.CHERISHED, SOME_SUBTYPE)),
            new TestCase(new Given(OTHER, SOME_SUBTYPE), new Expect(DocumentType.OTHER, SOME_SUBTYPE)),
            new TestCase(new Given(FORM, SOME_SUBTYPE), new Expect(DocumentType.FORM, SOME_SUBTYPE))
        ).forEach(tc -> {
            // given
            InputScannableItem item = inputScannableItem(tc.input.documentType, tc.input.docSubtype);

            // when
            ScannableItem result = EnvelopeMapper.toDbScannableItem(item, null);

            // then
            SoftAssertions softly = new SoftAssertions();

            softly.assertThat(result.getDocumentType())
                .as("Output type for type '%s' and subtype '%s'", tc.input.documentType, tc.input.docSubtype)
                .isEqualTo(tc.expected.documentType);

            softly.assertThat(result.getDocumentSubtype())
                .as("Output subtype for type '%s' and subtype '%s'", tc.input.documentType, tc.input.docSubtype)
                .isEqualTo(tc.expected.docSubtype);

            softly.assertAll();
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
        public String docSubtype;

        public Given(InputDocumentType documentType, String docSubtype) {
            this.documentType = documentType;
            this.docSubtype = docSubtype;
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
