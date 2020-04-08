package uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.config.IntegrationTest;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.UnableToUploadDocumentException;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.BlobProcessorTask;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.ProcessorTestSuite;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.PROCESSED;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.UPLOAD_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.DirectoryZipper.zipDir;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.DOC_PROCESSED;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.DOC_UPLOADED;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.DOC_UPLOAD_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.ZIPFILE_PROCESSING_STARTED;

@IntegrationTest
@RunWith(SpringRunner.class)
public class FailedDocUploadProcessorTest extends ProcessorTestSuite<FailedDocUploadProcessor> {

    private BlobProcessorTask blobProcessorTask;

    private static final String JURISDICTION = "BULKSCAN";

    @Before
    public void setUp() throws Exception {
        super.setUp();

        processor = new FailedDocUploadProcessor(
            blobManager,
            documentProcessor,
            envelopeProcessor,
            zipFileProcessor,
            envelopeRepository,
            processEventRepository
        );

        blobProcessorTask = new BlobProcessorTask(
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
    public void should_successfully_reupload_documents_when_initial_blob_processor_was_unable_to_do_so()
        throws Exception {

        // given
        uploadToBlobStorage(SAMPLE_ZIP_FILE_NAME, zipDir("zipcontents/ok"));

        given(documentManagementService.uploadDocuments(any()))
            .willReturn(Collections.emptyMap())
            .willReturn(ImmutableMap.of(
                "1111002.pdf", DOCUMENT_URL2
            ));

        blobProcessorTask.processBlobs();

        // when
        processor.processJurisdiction(JURISDICTION);

        // then
        List<Envelope> dbEnvelopes = envelopeRepository.findAll();

        assertThat(dbEnvelopes)
            .hasSize(1)
            .extracting(envelope -> envelope.getStatus())
            .containsOnlyOnce(PROCESSED);
        assertThat(dbEnvelopes.get(0).getScannableItems())
            .extracting(ScannableItem::getDocumentUuid)
            .hasSameElementsAs(ImmutableList.of(DOCUMENT_UUID2));

        // and
        String failureReason = "Error retrieving urls for uploaded files: 1111002.pdf";

        assertThat(processEventRepository.findAll())
            .extracting(e -> tuple(e.getContainer(), e.getZipFileName(), e.getEvent(), e.getReason()))
            .containsExactlyInAnyOrder(
                tuple(testContainer.getName(), SAMPLE_ZIP_FILE_NAME, ZIPFILE_PROCESSING_STARTED, null),
                tuple(testContainer.getName(), SAMPLE_ZIP_FILE_NAME, DOC_UPLOAD_FAILURE, failureReason),
                tuple(testContainer.getName(), SAMPLE_ZIP_FILE_NAME, DOC_UPLOADED, null),
                tuple(testContainer.getName(), SAMPLE_ZIP_FILE_NAME, DOC_PROCESSED, null)
            );
    }

    @Test
    public void should_fail_to_upload_pdfs_when_retrying_with_reupload_task() throws Exception {
        // given
        uploadToBlobStorage(SAMPLE_ZIP_FILE_NAME, zipDir("zipcontents/ok"));

        given(documentManagementService.uploadDocuments(any()))
            .willReturn(Collections.emptyMap()) // blob processor had empty response
            .willThrow(new UnableToUploadDocumentException("oh no", null)); // reupload task - failure

        blobProcessorTask.processBlobs();

        // when
        processor.processJurisdiction(JURISDICTION);

        // then
        assertThat(envelopeRepository.findAll())
            .hasSize(1)
            .extracting(envelope -> envelope.getStatus())
            .containsOnlyOnce(UPLOAD_FAILURE);

        // and
        String failureReason = "Error retrieving urls for uploaded files: 1111002.pdf";

        assertThat(processEventRepository.findAll())
            .hasSize(3)
            .extracting(e -> tuple(e.getContainer(), e.getZipFileName(), e.getEvent(), e.getReason()))
            .containsOnly(
                tuple(testContainer.getName(), SAMPLE_ZIP_FILE_NAME, ZIPFILE_PROCESSING_STARTED, null),
                tuple(testContainer.getName(), SAMPLE_ZIP_FILE_NAME, DOC_UPLOAD_FAILURE, failureReason),
                tuple(testContainer.getName(), SAMPLE_ZIP_FILE_NAME, DOC_UPLOAD_FAILURE, "oh no")
            );
    }

    @Test
    public void should_increment_upload_failure_count_if_unable_to_upload_files() throws Exception {
        // given
        uploadToBlobStorage(SAMPLE_ZIP_FILE_NAME, zipDir("zipcontents/ok"));

        given(documentManagementService.uploadDocuments(any()))
            .willThrow(UnableToUploadDocumentException.class);

        blobProcessorTask.processBlobs(); // original run

        // when
        processor.processJurisdiction(JURISDICTION); // retry run
        processor.processJurisdiction(JURISDICTION); // another retry run

        // then
        List<Envelope> envelopes = envelopeRepository.findAll();
        assertThat(envelopes).hasSize(1);

        Envelope envelope = envelopes.get(0);

        assertThat(envelope.getUploadFailureCount()).isEqualTo(3); // one original failure + 2 retry runs
    }
}
