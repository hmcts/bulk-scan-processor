package uk.gov.hmcts.reform.bulkscanprocessor.controllers.e2e;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import uk.gov.hmcts.reform.bulkscanprocessor.controllers.BaseFunctionalTest;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.EnvelopeResponse;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "RUN_END_TO_END_TESTS", matches = "true")
public class EnvelopeProcessingTest extends BaseFunctionalTest {

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void should_fully_process_envelope_with_exception_classification() {
        // given
        String destZipFilename = testHelper.getRandomFilename("11-05-2020-00-00-00.test.zip");

        // when
        uploadZipFile(
            asList("1111006.pdf", "1111002.pdf"),
            "exception_with_ocr_metadata.json",
            destZipFilename
        );

        // then
        EnvelopeResponse envelope = waitForEnvelopeToBeInStatus(destZipFilename, asList(Status.COMPLETED));

        assertThat(envelope.getScannableItems())
            .as("All OCR data must be cleared")
            .allMatch(item -> item.ocrData == null);
    }
}
