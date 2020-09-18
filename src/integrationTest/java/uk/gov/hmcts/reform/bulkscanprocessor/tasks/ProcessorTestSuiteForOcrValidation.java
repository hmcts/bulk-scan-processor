package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.HttpServerErrorException;
import org.testcontainers.containers.DockerComposeContainer;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.bulkscanprocessor.config.ContainerMappings;
import uk.gov.hmcts.reform.bulkscanprocessor.config.IntegrationTest;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client.OcrValidationClient;
import uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.ServiceBusHelper;
import uk.gov.hmcts.reform.bulkscanprocessor.services.storage.OcrValidationRetryManager;
import uk.gov.hmcts.reform.bulkscanprocessor.util.TestStorageHelper;
import uk.gov.hmcts.reform.bulkscanprocessor.validation.OcrPresenceValidator;
import uk.gov.hmcts.reform.bulkscanprocessor.validation.OcrValidator;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@IntegrationTest
@TestPropertySource(properties = {
    "scheduling.task.scan.enabled=true"
})
abstract class ProcessorTestSuiteForOcrValidation {
    static final String SAMPLE_ZIP_FILE_NAME = "1_24-06-2018-00-00-00.zip";

    static final String CONTAINER_NAME = "bulkscan";

    @Autowired
    BlobProcessorTask processor;

    @Autowired
    ContainerMappings containerMappings;

    @Autowired
    EnvelopeRepository envelopeRepository;

    @Autowired
    ProcessEventRepository processEventRepository;

    @Autowired
    OcrValidationRetryManager ocrValidationRetryManager;

    @Autowired
    OcrPresenceValidator ocrPresenceValidator;

    @MockBean
    AuthTokenGenerator authTokenGenerator;

    @MockBean
    OcrValidationClient ocrValidationClient;

    OcrValidator ocrValidator;

    BlobContainerClient testContainer;

    static DockerComposeContainer dockerComposeContainer;
    static String dockerHost;

    @BeforeEach
    void setUp() {

        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
            .connectionString(String.format(TestStorageHelper.STORAGE_CONN_STRING, dockerHost, 10000))
            .buildClient();

        ocrValidator = new OcrValidator(
            ocrValidationClient,
            ocrPresenceValidator,
            containerMappings,
            authTokenGenerator,
            ocrValidationRetryManager
        );

        testContainer = blobServiceClient.getBlobContainerClient(CONTAINER_NAME);
        if (!testContainer.exists()) {
            testContainer.create();
        }
    }

    @AfterEach
    void cleanUp() {
        if (testContainer.exists()) {
            testContainer.delete();
        }

        envelopeRepository.deleteAll();
        processEventRepository.deleteAll();
    }

    @BeforeAll
    static void initialize() {
        File dockerComposeFile = new File("src/integrationTest/resources/docker-compose.yml");

        dockerComposeContainer = new DockerComposeContainer(dockerComposeFile)
            .withExposedService("azure-storage", 10000)
            .withLocalCompose(true);

        dockerComposeContainer.start();
        dockerHost = dockerComposeContainer.getServiceHost("azure-storage", 10000);
    }

    @AfterAll
    static void tearDownContainer() {
        dockerComposeContainer.stop();
    }

    @NotNull
    HttpServerErrorException getServerSideException() {
        return HttpServerErrorException.create(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "internal server error message",
            HttpHeaders.EMPTY,
            null,
            null
        );
    }

    void uploadToBlobStorage(String fileName, byte[] fileContent) {
        var blockBlobReference = testContainer.getBlobClient(fileName);

        // Blob need to be deleted as same blob may exists if previously uploaded blob was not deleted
        // due to doc upload failure
        if (blockBlobReference.exists()) {
            blockBlobReference.delete();
        }

        // A Put Blob operation may succeed against a blob that exists in the storage emulator with an active lease,
        // even if the lease ID has not been specified in the request.
        blockBlobReference.upload(new ByteArrayInputStream(fileContent), fileContent.length);
    }

    Envelope getSingleEnvelopeFromDb() {
        // We expect only one envelope which was uploaded
        List<Envelope> envelopes = envelopeRepository.findAll();
        assertThat(envelopes).hasSize(1);

        return envelopes.get(0);
    }

    void assertNoEnvelopesInDb() {
        // We expect only one envelope which was uploaded
        List<Envelope> envelopes = envelopeRepository.findAll();
        assertThat(envelopes).hasSize(0);
    }

    void doSleep(long l) {
        try {
            Thread.sleep(l);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    void retryAfterDelay() {
        assertNoEnvelopesInDb();

        doSleep(5000L);

        processor.processBlobs();
    }

    @TestConfiguration
    public static class MockConfig {

        @Bean(name = "notifications-helper")
        public ServiceBusHelper notificationsQueueHelper() {
            return mock(ServiceBusHelper.class);
        }
    }
}
