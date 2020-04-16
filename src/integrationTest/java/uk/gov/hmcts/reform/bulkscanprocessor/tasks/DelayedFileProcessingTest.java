package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.config.IntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.DirectoryZipper.zipDir;

@IntegrationTest
@RunWith(SpringRunner.class)
public class DelayedFileProcessingTest extends ProcessorTestSuite<BlobProcessorTask> {

    @Before
    public void setUp() throws Exception {
        super.setUp();

        processor = new BlobProcessorTask(
            blobManager,
            documentProcessor,
            envelopeProcessor,
            zipFileProcessor,
            envelopeRepository,
            processEventRepository,
            containerMappings,
            ocrValidator,
            serviceBusHelper,
            paymentsEnabled
        );
    }

    @Test
    public void should_not_process_file_if_delay_is_greater_than_zero() throws Exception {
        // given
        uploadToBlobStorage(SAMPLE_ZIP_FILE_NAME, zipDir("zipcontents/ok"));
        processor.blobProcessingDelayInMinutes = 5;

        // when
        processor.processBlobs();

        // then
        assertThat(envelopeRepository.findAll()).hasSize(0); // file not processed -> no envelopes created

        // but given
        processor.blobProcessingDelayInMinutes = 0;

        // when
        processor.processBlobs();

        // then
        assertThat(envelopeRepository.findAll()).hasSize(1); // file processed -> envelope created
    }
}
