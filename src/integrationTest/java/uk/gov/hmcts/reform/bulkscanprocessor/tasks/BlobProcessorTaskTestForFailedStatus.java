package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.google.common.collect.ImmutableList;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.palantir.docker.compose.DockerComposeRule;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.ObjectUtils;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItemRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.UnableToUploadDocumentException;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.DocumentManagementService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.output.Pdf;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.DocumentProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static com.google.common.io.Resources.getResource;
import static com.google.common.io.Resources.toByteArray;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Event.DOC_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Event.DOC_UPLOAD_FAILURE;

@RunWith(SpringRunner.class)
@SpringBootTest
public class BlobProcessorTaskTestForFailedStatus {

    private static final String ZIP_FILE_NAME_SUCCESS = "1_24-06-2018-00-00-00.zip";

    @ClassRule
    public static DockerComposeRule docker = DockerComposeRule.builder()
        .file("src/integrationTest/resources/docker-compose.yml")
        .build();

    private BlobProcessorTask blobProcessorTask;

    @Autowired
    private EnvelopeRepository envelopeRepository;

    @Autowired
    private ProcessEventRepository processEventRepository;

    @Autowired
    private ScannableItemRepository scannableItemRepository;

    @Mock
    private DocumentManagementService documentManagementService;

    private CloudBlobContainer testContainer;

    @Before
    public void setup() throws Exception {
        CloudStorageAccount account = CloudStorageAccount.parse("UseDevelopmentStorage=true");
        CloudBlobClient cloudBlobClient = account.createCloudBlobClient();

        DocumentProcessor documentProcessor = new DocumentProcessor(
            documentManagementService,
            scannableItemRepository
        );

        EnvelopeProcessor envelopeProcessor = new EnvelopeProcessor(
            envelopeRepository,
            processEventRepository
        );

        blobProcessorTask = new BlobProcessorTask(
            cloudBlobClient,
            documentProcessor,
            envelopeProcessor
        );

        testContainer = cloudBlobClient.getContainerReference("test");
        testContainer.createIfNotExists();
    }

    @After
    public void cleanUp() throws Exception {
        testContainer.deleteIfExists();
        envelopeRepository.deleteAll();
        processEventRepository.deleteAll();
    }

    @Test
    public void should_record_failure_of_upload_when_document_management_returns_empty_response() throws Exception {
        // given
        uploadZipToBlobStore(ZIP_FILE_NAME_SUCCESS);

        // and
        given(documentManagementService.uploadDocuments(getUploadResources())).willReturn(Collections.emptyMap());

        // when
        blobProcessorTask.processBlobs();

        // then
        Envelope actualEnvelope = envelopeRepository.findAll().get(0);

        assertThat(actualEnvelope.getStatus()).isEqualTo(DOC_UPLOAD_FAILURE);
        assertThat(actualEnvelope.getScannableItems()).extracting("documentUrl").allMatch(ObjectUtils::isEmpty);

        // and
        List<ProcessEvent> processEvents = processEventRepository.findAll();
        assertThat(processEvents).hasSize(1);

        ProcessEvent processEvent = processEvents.get(0);
        assertThat(processEvent)
            .extracting("container", "zipFileName", "event")
            .hasSameElementsAs(ImmutableList.of(testContainer.getName(), ZIP_FILE_NAME_SUCCESS, DOC_UPLOAD_FAILURE));
        assertThat(processEvent.getId()).isNotNull();
        assertThat(processEvent.getReason()).isNotBlank();
    }

    @Test
    public void should_record_failure_of_upload_when_document_management_throws_exception() throws Exception {
        // given
        uploadZipToBlobStore(ZIP_FILE_NAME_SUCCESS);

        // and
        Throwable throwable = new UnableToUploadDocumentException("oh no", null);
        given(documentManagementService.uploadDocuments(getUploadResources())).willThrow(throwable);

        // when
        blobProcessorTask.processBlobs();

        // then
        Envelope actualEnvelope = envelopeRepository.findAll().get(0);

        assertThat(actualEnvelope.getStatus()).isEqualTo(DOC_UPLOAD_FAILURE);
        assertThat(actualEnvelope.getScannableItems()).extracting("documentUrl").allMatch(ObjectUtils::isEmpty);

        // and
        List<ProcessEvent> processEvents = processEventRepository.findAll();
        assertThat(processEvents).hasSize(1);

        ProcessEvent processEvent = processEvents.get(0);
        assertThat(processEvent)
            .extracting("container", "zipFileName", "event")
            .hasSameElementsAs(ImmutableList.of(testContainer.getName(), ZIP_FILE_NAME_SUCCESS, DOC_UPLOAD_FAILURE));
        assertThat(processEvent.getId()).isNotNull();
        assertThat(processEvent.getReason()).isEqualTo(throwable.getMessage());
    }

    @Test
    public void should_record_generic_failure_when_zip_does_not_contain_metadata_json() throws Exception {
        // given
        String noMetafileZip = "2_24-06-2018-00-00-00.zip";
        uploadZipToBlobStore(noMetafileZip); //Zip file with only pdfs and no metadata

        // when
        blobProcessorTask.processBlobs();

        // then
        List<Envelope> envelopesInDb = envelopeRepository.findAll();
        assertThat(envelopesInDb).isEmpty();

        // and
        List<ProcessEvent> processEvents = processEventRepository.findAll();
        assertThat(processEvents).hasSize(1);

        ProcessEvent processEvent = processEvents.get(0);
        assertThat(processEvent)
            .extracting("container", "zipFileName", "event")
            .hasSameElementsAs(ImmutableList.of(testContainer.getName(), noMetafileZip, DOC_FAILURE));
        assertThat(processEvent.getId()).isNotNull();
        assertThat(processEvent.getReason()).isNotBlank();
    }

    @Test
    public void should_record_generic_failure_when_metadata_parsing_fails() throws Exception {
        // given
        String invalidMetafileZip = "6_24-06-2018-00-00-00.zip";
        uploadZipToBlobStore(invalidMetafileZip); //Zip file with pdf and invalid metadata

        // when
        blobProcessorTask.processBlobs();

        // then
        List<Envelope> envelopesInDb = envelopeRepository.findAll();
        assertThat(envelopesInDb).isEmpty();

        // and
        List<ProcessEvent> processEvents = processEventRepository.findAll();
        assertThat(processEvents).hasSize(1);

        ProcessEvent processEvent = processEvents.get(0);
        assertThat(processEvent)
            .extracting("container", "zipFileName", "event")
            .hasSameElementsAs(ImmutableList.of(testContainer.getName(), invalidMetafileZip, DOC_FAILURE));
        assertThat(processEvent.getId()).isNotNull();
        assertThat(processEvent.getReason()).isNotBlank();
    }

    @Test
    public void should_record_generic_failure_when_zip_contains_documents_not_in_pdf_format() throws Exception {
        // given
        String noPdfZip = "5_24-06-2018-00-00-00.zip";
        uploadZipToBlobStore(noPdfZip); // Zip file with cheque gif and metadata

        // when
        blobProcessorTask.processBlobs();

        // then
        List<Envelope> envelopesInDb = envelopeRepository.findAll();
        assertThat(envelopesInDb).isEmpty();

        // and
        List<ProcessEvent> processEvents = processEventRepository.findAll();
        assertThat(processEvents).hasSize(1);

        ProcessEvent processEvent = processEvents.get(0);
        assertThat(processEvent)
            .extracting("container", "zipFileName", "event")
            .hasSameElementsAs(ImmutableList.of(testContainer.getName(), noPdfZip, DOC_FAILURE));
        assertThat(processEvent.getId()).isNotNull();
        assertThat(processEvent.getReason()).isNotBlank();
    }

    private void uploadZipToBlobStore(String zipFileName) throws Exception {
        byte[] zipFile = toByteArray(getResource(zipFileName));

        CloudBlockBlob blockBlobReference = testContainer.getBlockBlobReference(zipFileName);
        blockBlobReference.uploadFromByteArray(zipFile, 0, zipFile.length);
    }

    private List<Pdf> getUploadResources() throws IOException {
        byte[] test1PdfBytes = toByteArray(getResource("1111001.pdf"));
        byte[] test2PdfBytes = toByteArray(getResource("1111002.pdf"));

        Pdf pdf1 = new Pdf("1111001.pdf", test1PdfBytes);
        Pdf pdf2 = new Pdf("1111002.pdf", test2PdfBytes);

        return ImmutableList.of(pdf1, pdf2);
    }
}
