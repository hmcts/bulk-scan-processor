package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.microsoft.azure.storage.blob.CloudBlockBlob;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.config.IntegrationTest;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorCode;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorMsg;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.DirectoryZipper.zipDir;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.FILE_VALIDATION_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.ZIPFILE_PROCESSING_STARTED;

@IntegrationTest
@RunWith(SpringRunner.class)
@TestPropertySource(properties = {
    "containers.mappings[0].container=bulkscan",
    "containers.mappings[0].jurisdiction=BULKSCAN",
    "containers.mappings[0].poBox=BULKSCANPO",
    "containers.mappings[0].paymentsEnabled=false",
    "containers.mappings[0].enabled=false"
})
public class BlobProcessorTaskTestForFailingNotification extends ProcessorTestSuite<BlobProcessorTask> {

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
    public void should_record_error_when_ocr_data_missing_for_supplementary_evidence_with_ocr() throws Exception {
        // given
        uploadToBlobStorage(SAMPLE_ZIP_FILE_NAME,
                            zipDir("zipcontents/supplementary_evidence_with_ocr_missing_ocr_data")); // no ocr data

        doThrow(new RuntimeException("error message"))
            .when(serviceBusHelper).sendMessage(any(ErrorMsg.class));

        // when
        processor.processBlobs();

        // then
        envelopeWasNotCreated();
        eventsWereCreated(ZIPFILE_PROCESSING_STARTED, FILE_VALIDATION_FAILURE);
        errorWasSent(SAMPLE_ZIP_FILE_NAME, ErrorCode.ERR_METAFILE_INVALID);
        CloudBlockBlob blob = testContainer.getBlockBlobReference(SAMPLE_ZIP_FILE_NAME);
        assertThat(blob.exists()).isTrue();
        verify(serviceBusHelper).sendMessage(any(ErrorMsg.class));
    }
}
