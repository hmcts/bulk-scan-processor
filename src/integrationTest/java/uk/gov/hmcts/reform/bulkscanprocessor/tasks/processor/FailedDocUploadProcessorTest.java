package uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor;

import com.google.common.collect.ImmutableList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.UnableToUploadDocumentException;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.output.Pdf;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.BlobProcessorTask;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.ProcessorTestSuite;

import java.util.Collections;
import java.util.List;

import static com.google.common.io.Resources.getResource;
import static com.google.common.io.Resources.toByteArray;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Event.DOC_PROCESSED;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Event.DOC_UPLOADED;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Event.DOC_UPLOAD_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.PROCESSED;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.UPLOAD_FAILURE;

@RunWith(SpringRunner.class)
@SpringBootTest
public class FailedDocUploadProcessorTest extends ProcessorTestSuite<FailedDocUploadProcessor> {

    private BlobProcessorTask blobProcessorTask;

    @Before
    public void setUp() throws Exception {
        super.setUp(FailedDocUploadProcessor::new);

        blobProcessorTask = new BlobProcessorTask(
            testContainer.getServiceClient(),
            documentProcessor,
            envelopeProcessor,
            errorWrapper
        );
    }

    @After
    public void tearDown() {
        envelopeRepository.deleteAll();
        processEventRepository.deleteAll();
    }

    @Test
    public void should_successfully_reupload_documents_when_initial_blob_processor_was_unable_to_do_so()
        throws Exception {
        // given
        uploadZipToBlobStore(ZIP_FILE_NAME_SUCCESS); //Zip file with metadata and pdfs

        byte[] test1PdfBytes = toByteArray(getResource("1111001.pdf"));
        byte[] test2PdfBytes = toByteArray(getResource("1111002.pdf"));

        Pdf pdf1 = new Pdf("1111001.pdf", test1PdfBytes);
        Pdf pdf2 = new Pdf("1111002.pdf", test2PdfBytes);

        given(documentManagementService.uploadDocuments(ImmutableList.of(pdf1, pdf2)))
            .willReturn(Collections.emptyMap())
            .willReturn(getFileUploadResponse());

        blobProcessorTask.processBlobs();

        // when
        processor.processJurisdiction("SSCS");

        // then
        List<Envelope> dbEnvelopes = envelopeRepository.findAll();

        assertThat(dbEnvelopes)
            .hasSize(1)
            .extracting("status")
            .containsOnlyOnce(PROCESSED);
        assertThat(dbEnvelopes.get(0).getScannableItems())
            .extracting("documentUrl")
            .hasSameElementsAs(ImmutableList.of(DOCUMENT_URL1, DOCUMENT_URL2));

        // and
        String failureReason = "Document metadata not found for file " + pdf1.getFilename();

        assertThat(processEventRepository.findAll())
            .hasSize(3)
            .extracting("container", "zipFileName", "event", "reason")
            .containsOnly(
                tuple(testContainer.getName(), ZIP_FILE_NAME_SUCCESS, DOC_UPLOAD_FAILURE, failureReason),
                tuple(testContainer.getName(), ZIP_FILE_NAME_SUCCESS, DOC_UPLOADED, null),
                tuple(testContainer.getName(), ZIP_FILE_NAME_SUCCESS, DOC_PROCESSED, null)
            );
    }

    @Test
    public void should_fail_to_upload_pdfs_when_retrying_with_reupload_task() throws Exception {
        // given
        uploadZipToBlobStore(ZIP_FILE_NAME_SUCCESS); //Zip file with metadata and pdfs

        byte[] test1PdfBytes = toByteArray(getResource("1111001.pdf"));
        byte[] test2PdfBytes = toByteArray(getResource("1111002.pdf"));

        Pdf pdf1 = new Pdf("1111001.pdf", test1PdfBytes);
        Pdf pdf2 = new Pdf("1111002.pdf", test2PdfBytes);

        given(documentManagementService.uploadDocuments(ImmutableList.of(pdf1, pdf2)))
            .willReturn(Collections.emptyMap()) // blob processor had empty response
            .willThrow(new UnableToUploadDocumentException("oh no", null)); // reupload task - failure

        blobProcessorTask.processBlobs();

        // when
        processor.processJurisdiction("SSCS");

        // then
        assertThat(envelopeRepository.findAll())
            .hasSize(1)
            .extracting("status")
            .containsOnlyOnce(UPLOAD_FAILURE);

        // and
        String failureReason = "Document metadata not found for file " + pdf1.getFilename();

        assertThat(processEventRepository.findAll())
            .hasSize(2)
            .extracting("container", "zipFileName", "event", "reason")
            .containsOnly(
                tuple(testContainer.getName(), ZIP_FILE_NAME_SUCCESS, DOC_UPLOAD_FAILURE, failureReason),
                tuple(testContainer.getName(), ZIP_FILE_NAME_SUCCESS, DOC_UPLOAD_FAILURE, "oh no")
            );
    }
}
