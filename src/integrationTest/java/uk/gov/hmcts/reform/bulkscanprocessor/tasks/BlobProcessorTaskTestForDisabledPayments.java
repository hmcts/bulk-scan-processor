package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.reform.bulkscanprocessor.config.IntegrationTest;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorCode;

import java.io.File;

import static com.google.common.io.Resources.getResource;
import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.DirectoryZipper.zipDir;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.FILE_VALIDATION_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.ZIPFILE_PROCESSING_STARTED;

@IntegrationTest
@TestPropertySource(properties = {
    "containers.mappings[0].container=bulkscan",
    "containers.mappings[0].jurisdiction=BULKSCAN",
    "containers.mappings[0].poBoxes[0]=BULKSCANPO",
    "containers.mappings[0].paymentsEnabled=false",
    "containers.mappings[0].enabled=true"
})
@Disabled
public class BlobProcessorTaskTestForDisabledPayments extends ProcessorTestSuite {


    @Test
    public void should_reject_file_with_payments_when_payments_are_disabled() throws Exception {
        // given
        byte[] zipBytes = zipDir("zipcontents/disabled_payments");

        uploadToBlobStorage(SAMPLE_ZIP_FILE_NAME, zipBytes);

        // and
        File pdf = new File(getResource("zipcontents/disabled_payments/1111002.pdf").toURI());


        given(documentManagementService.uploadDocuments(ImmutableList.of(pdf), "BULKSCAN", "bulkscan"))
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

}
