package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.config.IntegrationTest;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorCode;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorMsg;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.Msg;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.output.Pdf;

import static com.google.common.io.Resources.getResource;
import static com.google.common.io.Resources.toByteArray;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
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
    "containers.mappings[0].enabled=true"
})
public class BlobProcessorTaskTestForDisabledPayments extends ProcessorTestSuite<BlobProcessorTask> {

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
            paymentsEnabled,
            retryCount
        );
    }

    @Test
    public void should_reject_file_with_payments_when_payments_are_disabled() throws Exception {
        // given
        byte[] zipBytes = zipDir("zipcontents/disabled_payments");

        uploadToBlobStorage(SAMPLE_ZIP_FILE_NAME, zipBytes);

        // and
        Pdf pdf = new Pdf(
            "1111002.pdf",
            toByteArray(getResource("zipcontents/disabled_payments/1111002.pdf"))
        );

        given(documentManagementService.uploadDocuments(ImmutableList.of(pdf)))
            .willReturn(ImmutableMap.of(
                "1111002.pdf", DOCUMENT_URL2
            ));

        // when
        processor.processBlobs();

        // then
        envelopeWasNotCreated();
        eventsWereCreated(ZIPFILE_PROCESSING_STARTED, FILE_VALIDATION_FAILURE);
        fileWasDeleted(SAMPLE_ZIP_FILE_NAME);
        errorWasSent(SAMPLE_ZIP_FILE_NAME, ErrorCode.ERR_PAYMENTS_DISABLED);
    }

    @Test
    public void should_reject_file_with_payments_when_payments_are_disabled_and_retry_send_message() throws Exception {
        // given
        byte[] zipBytes = zipDir("zipcontents/disabled_payments");

        uploadToBlobStorage(SAMPLE_ZIP_FILE_NAME, zipBytes);

        // and
        Pdf pdf = new Pdf(
            "1111002.pdf",
            toByteArray(getResource("zipcontents/disabled_payments/1111002.pdf"))
        );

        given(documentManagementService.uploadDocuments(ImmutableList.of(pdf)))
            .willReturn(ImmutableMap.of(
                "1111002.pdf", DOCUMENT_URL2
            ));
        doThrow(new RuntimeException())
            .doNothing()
            .when(serviceBusHelper).sendMessage(any(Msg.class));

        // when
        processor.processBlobs();

        // then
        envelopeWasNotCreated();
        eventsWereCreated(ZIPFILE_PROCESSING_STARTED, FILE_VALIDATION_FAILURE);
        fileWasDeleted(SAMPLE_ZIP_FILE_NAME);
        sendMessageWasCalledSeveralTimes(SAMPLE_ZIP_FILE_NAME, ErrorCode.ERR_PAYMENTS_DISABLED, 2);
    }

    @Test
    public void should_reject_file_with_payments_when_payments_are_disabled_and_retry_send_message_twice()
        throws Exception {
        // given
        byte[] zipBytes = zipDir("zipcontents/disabled_payments");

        uploadToBlobStorage(SAMPLE_ZIP_FILE_NAME, zipBytes);

        // and
        Pdf pdf = new Pdf(
            "1111002.pdf",
            toByteArray(getResource("zipcontents/disabled_payments/1111002.pdf"))
        );

        given(documentManagementService.uploadDocuments(ImmutableList.of(pdf)))
            .willReturn(ImmutableMap.of(
                "1111002.pdf", DOCUMENT_URL2
            ));
        doThrow(new RuntimeException())
            .doThrow(new RuntimeException())
            .doThrow(new RuntimeException())
            .doThrow(new RuntimeException())
            .doNothing()
            .when(serviceBusHelper).sendMessage(any(Msg.class));

        // when
        processor.processBlobs();

        // then
        envelopeWasNotCreated();
        eventsWereCreated(ZIPFILE_PROCESSING_STARTED, FILE_VALIDATION_FAILURE);
        fileWasDeleted(SAMPLE_ZIP_FILE_NAME);
        sendMessageWasCalledSeveralTimes(SAMPLE_ZIP_FILE_NAME, ErrorCode.ERR_PAYMENTS_DISABLED, retryCount + 1);
    }

    private void sendMessageWasCalledSeveralTimes(String zipFileName, ErrorCode code, int numberOfTimes) {
        ArgumentCaptor<ErrorMsg> argument = ArgumentCaptor.forClass(ErrorMsg.class);
        verify(serviceBusHelper, times(numberOfTimes)).sendMessage(argument.capture());

        ErrorMsg sentMsg = argument.getValue();

        assertThat(sentMsg.zipFileName).isEqualTo(zipFileName);
        assertThat(sentMsg.jurisdiction).isEqualTo(CONTAINER_NAME);
        assertThat(sentMsg.errorCode).isEqualTo(code);
        assertThat(sentMsg.poBox).isEqualTo(PO_BOX);
        assertThat(sentMsg.container).isEqualTo(CONTAINER_NAME);
        assertThat(sentMsg.documentControlNumber).isNull();
        assertThat(sentMsg.service).isEqualTo("bulk_scan_processor");
        assertThat(sentMsg.errorCode).isEqualTo(code);
        assertThat(sentMsg.errorDescription).isNotEmpty();
    }
}
