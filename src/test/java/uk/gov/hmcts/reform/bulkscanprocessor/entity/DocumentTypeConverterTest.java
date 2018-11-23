package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.test.rule.OutputCapture;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.DocumentType.CHERISHED;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.DocumentType.OTHER;

public class DocumentTypeConverterTest {

    private static final DocumentTypeConverter CONVERTER = new DocumentTypeConverter();

    @Rule
    public OutputCapture capture = new OutputCapture();

    @After
    public void cleanUp() {
        capture.flush();
    }

    @Test
    public void should_convert_to_lower_case_for_db_entry() {
        assertThat(CONVERTER.convertToDatabaseColumn(OTHER)).isEqualTo(OTHER.name().toLowerCase());
    }

    @Test
    public void should_get_cherished_document_type_from_correct_db_entry() {
        assertThat(CONVERTER.convertToEntityAttribute("cherished")).isEqualTo(CHERISHED);
    }

    @Test
    public void should_not_fail_and_return_other_for_invalid_document_type_found_in_db() {
        assertThat(CONVERTER.convertToEntityAttribute("not valid")).isEqualTo(OTHER);

        assertThat(capture.toString()).containsSequence("Invalid document type found in DB: 'not valid'");
    }
}
