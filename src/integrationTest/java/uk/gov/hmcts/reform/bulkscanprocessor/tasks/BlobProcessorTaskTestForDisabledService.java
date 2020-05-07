package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.config.IntegrationTest;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorCode;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorMsg;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.output.Pdf;

import static com.google.common.io.Resources.getResource;
import static com.google.common.io.Resources.toByteArray;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.DirectoryZipper.zipDir;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.DISABLED_SERVICE_FAILURE;
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
public class BlobProcessorTaskTestForDisabledService extends ProcessorTestSuite<BlobProcessorTask> {

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
    public void should_reject_file_when_service_is_disabled() throws Exception {
        // given
        byte[] zipBytes = zipDir("zipcontents/ok");

        uploadToBlobStorage(SAMPLE_ZIP_FILE_NAME, zipBytes);

        // and
        Pdf pdf = new Pdf(
            "1111002.pdf",
            toByteArray(getResource("zipcontents/ok/1111002.pdf"))
        );

        given(documentManagementService.uploadDocuments(ImmutableList.of(pdf)))
            .willReturn(ImmutableMap.of(
                "1111002.pdf", DOCUMENT_URL2
            ));

        // when
        processor.processBlobs();

        // then
        envelopeWasNotCreated();
        eventsWereCreated(ZIPFILE_PROCESSING_STARTED, DISABLED_SERVICE_FAILURE);
        fileWasDeleted(SAMPLE_ZIP_FILE_NAME);
        errorWasSent(SAMPLE_ZIP_FILE_NAME, ErrorCode.ERR_SERVICE_DISABLED);
    }

    @Test
    public void should_not_move_file_if_send_message_throws_exception_when_service_is_disabled() throws Exception {
        // given
        byte[] zipBytes = zipDir("zipcontents/ok");

        uploadToBlobStorage(SAMPLE_ZIP_FILE_NAME, zipBytes);

        // and
        Pdf pdf = new Pdf(
            "1111002.pdf",
            toByteArray(getResource("zipcontents/ok/1111002.pdf"))
        );

        doThrow(new RuntimeException("error message"))
            .when(serviceBusHelper).sendMessage(any(ErrorMsg.class));
        given(documentManagementService.uploadDocuments(ImmutableList.of(pdf)))
            .willReturn(ImmutableMap.of(
                "1111002.pdf", DOCUMENT_URL2
            ));

        // when
        processor.processBlobs();

        // then
        envelopeWasNotCreated();
        eventsWereCreated(ZIPFILE_PROCESSING_STARTED, DISABLED_SERVICE_FAILURE);
        CloudBlockBlob blob = testContainer.getBlockBlobReference(SAMPLE_ZIP_FILE_NAME);
        assertThat(blob.exists()).isTrue();
        errorWasSent(SAMPLE_ZIP_FILE_NAME, ErrorCode.ERR_SERVICE_DISABLED);
        verify(serviceBusHelper).sendMessage(any(ErrorMsg.class));
    }

}
