package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.reform.bulkscanprocessor.config.IntegrationTest;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorCode;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.output.Pdf;

import java.io.File;

import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.DirectoryZipper.zipDir;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.DISABLED_SERVICE_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.ZIPFILE_PROCESSING_STARTED;
import static uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.ZipFileProcessor.DOWNLOAD_PATH;

@IntegrationTest
@TestPropertySource(properties = {
    "containers.mappings[0].container=bulkscan",
    "containers.mappings[0].jurisdiction=BULKSCAN",
    "containers.mappings[0].poBox=BULKSCANPO",
    "containers.mappings[0].paymentsEnabled=false",
    "containers.mappings[0].enabled=false"
})
public class BlobProcessorTaskTestForDisabledService extends ProcessorTestSuite {

    @Test
    public void should_reject_file_when_service_is_disabled() throws Exception {
        // given
        byte[] zipBytes = zipDir("zipcontents/ok");

        uploadToBlobStorage(SAMPLE_ZIP_FILE_NAME, zipBytes);

        // and
        Pdf pdf = new Pdf(
            "1111002.pdf",
            new File(DOWNLOAD_PATH + SAMPLE_ZIP_FILE_NAME +  "1111002.pdf")
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

}
