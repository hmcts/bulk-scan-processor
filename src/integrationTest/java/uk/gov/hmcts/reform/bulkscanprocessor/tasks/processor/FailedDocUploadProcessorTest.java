package uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEvent;
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

    @Test
    public void should_read_from_blob_storage_and_save_metadata_in_database_when_zip_contains_metadata_and_pdfs()
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

        // and
        uploadZipToBlobStore(ZIP_FILE_NAME_SUCCESS);

        // when
        processor.processJurisdiction("SSCS");

        // then
        Envelope dbEnvelope = envelopeRepository.findAll().get(0);

        assertThat(dbEnvelope.getStatus()).isEqualTo(PROCESSED);
        assertThat(dbEnvelope.getScannableItems())
            .extracting("documentUrl")
            .hasSameElementsAs(ImmutableList.of(DOCUMENT_URL1, DOCUMENT_URL2));

        // and
        List<ProcessEvent> processEvents = processEventRepository.findAll();
        assertThat(processEvents).hasSize(3);

        String failureReason = "Document metadata not found for file " + pdf1.getFilename();

        assertThat(processEvents)
            .extracting("container", "zipFileName", "event", "reason")
            .contains(
                tuple(testContainer.getName(), ZIP_FILE_NAME_SUCCESS, DOC_UPLOAD_FAILURE, failureReason),
                tuple(testContainer.getName(), ZIP_FILE_NAME_SUCCESS, DOC_UPLOADED, null),
                tuple(testContainer.getName(), ZIP_FILE_NAME_SUCCESS, DOC_PROCESSED, null)
            );
    }
}
