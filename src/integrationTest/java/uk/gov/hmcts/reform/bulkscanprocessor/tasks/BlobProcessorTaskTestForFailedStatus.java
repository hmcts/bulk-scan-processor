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
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeState;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeStateRepository;
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
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeStatus.DOC_UPLOAD_FAILURE;

@RunWith(SpringRunner.class)
@SpringBootTest
public class BlobProcessorTaskTestForFailedStatus {

    private static final String ZIP_FILE_NAME = "1_24-06-2018-00-00-00.zip";

    @ClassRule
    public static DockerComposeRule docker = DockerComposeRule.builder()
        .file("src/integrationTest/resources/docker-compose.yml")
        .build();

    private BlobProcessorTask blobProcessorTask;

    @Autowired
    private EnvelopeRepository envelopeRepository;

    @Autowired
    private EnvelopeStateRepository envelopeStateRepository;

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
            envelopeStateRepository
        );

        blobProcessorTask = new BlobProcessorTask(
            cloudBlobClient,
            documentProcessor,
            envelopeProcessor
        );

        testContainer = cloudBlobClient.getContainerReference("test");
        testContainer.createIfNotExists();

        uploadZipToBlobStore();
    }

    @After
    public void cleanUp() throws Exception {
        testContainer.deleteIfExists();
        envelopeRepository.deleteAll();
        envelopeStateRepository.deleteAll();
    }

    @Test
    public void should_record_failure_of_upload_when_document_management_returns_empty_response() throws IOException {
        // given
        given(documentManagementService.uploadDocuments(getUploadResources())).willReturn(Collections.emptyMap());

        // when
        blobProcessorTask.processBlobs();

        // then
        Envelope actualEnvelope = envelopeRepository.findAll().get(0);

        assertThat(actualEnvelope.getScannableItems()).extracting("documentUrl").allMatch(ObjectUtils::isEmpty);

        // and
        List<EnvelopeState> envelopeStates = envelopeStateRepository.findAll();
        assertThat(envelopeStates).hasSize(1);

        EnvelopeState envelopeState = envelopeStates.get(0);
        assertThat(envelopeState)
            .extracting("container", "zipFileName", "status")
            .hasSameElementsAs(ImmutableList.of(testContainer.getName(), ZIP_FILE_NAME, DOC_UPLOAD_FAILURE));
        assertThat(envelopeState.getId()).isNotNull();
        assertThat(envelopeState.getReason()).isNotBlank();
        assertThat(envelopeState.getEnvelope().getId()).isEqualTo(actualEnvelope.getId());
    }

    @Test
    public void should_record_failure_of_upload_when_document_management_throws_exception() throws IOException {
        // given
        Throwable throwable = new UnableToUploadDocumentException("oh no", null);
        given(documentManagementService.uploadDocuments(getUploadResources())).willThrow(throwable);

        // when
        blobProcessorTask.processBlobs();

        // then
        Envelope actualEnvelope = envelopeRepository.findAll().get(0);

        assertThat(actualEnvelope.getScannableItems()).extracting("documentUrl").allMatch(ObjectUtils::isEmpty);

        // and
        List<EnvelopeState> envelopeStates = envelopeStateRepository.findAll();
        assertThat(envelopeStates).hasSize(1);

        EnvelopeState envelopeState = envelopeStates.get(0);
        assertThat(envelopeState)
            .extracting("container", "zipFileName", "status")
            .hasSameElementsAs(ImmutableList.of(testContainer.getName(), ZIP_FILE_NAME, DOC_UPLOAD_FAILURE));
        assertThat(envelopeState.getId()).isNotNull();
        assertThat(envelopeState.getReason()).isEqualTo(throwable.getMessage());
        assertThat(envelopeState.getEnvelope().getId()).isEqualTo(actualEnvelope.getId());
    }

    private void uploadZipToBlobStore() throws Exception {
        byte[] zipFile = toByteArray(getResource(ZIP_FILE_NAME));

        CloudBlockBlob blockBlobReference = testContainer.getBlockBlobReference(ZIP_FILE_NAME);
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
