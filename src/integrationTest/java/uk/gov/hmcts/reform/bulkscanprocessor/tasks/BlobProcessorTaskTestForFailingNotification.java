package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import org.junit.jupiter.api.Test;
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
public class BlobProcessorTaskTestForFailingNotification extends ProcessorTestSuite {

    @Test
    public void should_abandon_processing_invalid_file_when_sending_error_notification_fails() throws Exception {
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
        var blob = testContainer.getBlobClient(SAMPLE_ZIP_FILE_NAME);
        assertThat(blob.exists()).isTrue();
        verify(serviceBusHelper).sendMessage(any(ErrorMsg.class));
    }
}
