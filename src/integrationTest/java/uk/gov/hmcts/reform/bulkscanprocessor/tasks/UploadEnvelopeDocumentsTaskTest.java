package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.DockerComposeContainer;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.bulkscanprocessor.config.IntegrationTest;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputEnvelope;
import uk.gov.hmcts.reform.bulkscanprocessor.model.mapper.EnvelopeMapper;
import uk.gov.hmcts.reform.bulkscanprocessor.services.UploadEnvelopeDocumentsService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.DocumentManagementService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.output.Pdf;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;

import java.io.File;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.Optional;
import java.util.UUID;

import static com.google.common.io.Resources.getResource;
import static com.google.common.io.Resources.toByteArray;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.UPLOADED;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.DirectoryZipper.zipDir;

@IntegrationTest
@RunWith(SpringRunner.class)
public class UploadEnvelopeDocumentsTaskTest {

    private static final String CONTAINER_NAME = "bulkscan";
    private static final String ZIP_FILE_NAME = "1_24-06-2018-00-00-00.zip";
    private static final DockerComposeContainer DOCKER_CONTAINER = new DockerComposeContainer(
        new File("src/integrationTest/resources/docker-compose.yml")
    ).withExposedService("azure-storage", 10000);

    private static CloudBlobClient cloudBlobClient;

    @Autowired private EnvelopeProcessor envelopeProcessor;
    @Autowired private EnvelopeRepository envelopeRepository;
    @Autowired private ProcessEventRepository eventRepository;
    @Autowired private UploadEnvelopeDocumentsService uploadService;

    @MockBean private AuthTokenGenerator tokenGenerator;
    @MockBean private DocumentManagementService documentManagementService;

    private CloudBlobContainer testContainer;

    @BeforeClass
    public static void initializeContainer() throws InvalidKeyException, URISyntaxException {
        DOCKER_CONTAINER.start();
        cloudBlobClient = CloudStorageAccount
            .parse("UseDevelopmentStorage=true")
            .createCloudBlobClient();
    }

    @AfterClass
    public static void tearDownContainer() {
        DOCKER_CONTAINER.stop();
    }

    @Before
    public void prepare() throws URISyntaxException, StorageException {
        envelopeRepository.deleteAll();
        eventRepository.deleteAll();
        testContainer = cloudBlobClient.getContainerReference(CONTAINER_NAME);
        testContainer.createIfNotExists();
    }

    @After
    public void cleanUp() throws StorageException {
        testContainer.deleteIfExists();
        envelopeRepository.deleteAll();
        eventRepository.deleteAll();
    }

    @Test
    public void should_mark_envelope_as_uploaded() throws Exception {
        // given
        uploadToBlobStorage();

        // and
        Pdf pdf = new Pdf("1111002.pdf", toByteArray(getResource("zipcontents/ok/1111002.pdf")));

        given(tokenGenerator.generate()).willReturn("token");
        given(documentManagementService.uploadDocuments(ImmutableList.of(pdf)))
            .willReturn(ImmutableMap.of(
                "1111002.pdf", "http://localhost:8080/documents/" + UUID.randomUUID().toString()
            ));

        // and
        InputEnvelope inputEnvelope = envelopeProcessor.parseEnvelope(
            toByteArray(getResource("zipcontents/ok/metadata.json")),
            ZIP_FILE_NAME
        );
        Envelope envelope = EnvelopeMapper.toDbEnvelope(inputEnvelope, CONTAINER_NAME, Optional.empty());
        UUID envelopeId = envelopeRepository.saveAndFlush(envelope).getId();

        // when
        new UploadEnvelopeDocumentsTask(0, envelopeRepository, uploadService).run();

        // then
        assertThat(envelopeRepository.findById(envelopeId))
            .isNotEmpty()
            .get()
            .extracting(Envelope::getStatus)
            .isEqualTo(UPLOADED);
    }

    private void uploadToBlobStorage() throws Exception {
        CloudBlockBlob blockBlobReference = testContainer.getBlockBlobReference(ZIP_FILE_NAME);

        // Blob need to be deleted as same blob may exists if previously uploaded blob was not deleted
        // due to doc upload failure
        if (blockBlobReference.exists()) {
            blockBlobReference.breakLease(0);
            blockBlobReference.delete();
        }

        byte[] fileContent = zipDir("zipcontents/ok");
        blockBlobReference.uploadFromByteArray(fileContent, 0, fileContent.length);
    }
}
