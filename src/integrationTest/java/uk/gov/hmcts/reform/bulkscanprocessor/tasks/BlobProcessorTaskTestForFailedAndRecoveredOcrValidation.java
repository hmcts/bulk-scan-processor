package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.google.common.io.Resources;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;
import org.testcontainers.containers.DockerComposeContainer;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.bulkscanprocessor.config.ContainerMappings;
import uk.gov.hmcts.reform.bulkscanprocessor.config.IntegrationTest;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client.OcrValidationClient;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client.model.req.FormData;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client.model.res.ValidationResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.services.storage.OcrValidationRetryManager;
import uk.gov.hmcts.reform.bulkscanprocessor.util.TestStorageHelper;
import uk.gov.hmcts.reform.bulkscanprocessor.validation.OcrPresenceValidator;
import uk.gov.hmcts.reform.bulkscanprocessor.validation.OcrValidator;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.Charset;
import java.util.List;

import static com.google.common.io.Resources.getResource;
import static com.jayway.jsonpath.JsonPath.parse;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.CREATED;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.DirectoryZipper.zipDir;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.ZIPFILE_PROCESSING_STARTED;
import static uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client.model.res.Status.SUCCESS;

@IntegrationTest
class BlobProcessorTaskTestForFailedAndRecoveredOcrValidation {
    private static final String SAMPLE_ZIP_FILE_NAME = "1_24-06-2018-00-00-00.zip";

    private static final String CONTAINER_NAME = "bulkscan";

    @Autowired
    protected BlobProcessorTask processor;

    @Autowired
    private ContainerMappings containerMappings;

    @Autowired
    protected EnvelopeRepository envelopeRepository;

    @Autowired
    private ProcessEventRepository processEventRepository;

    @Autowired
    private OcrValidationRetryManager ocrValidationRetryManager;

    @Autowired
    private OcrPresenceValidator ocrPresenceValidator;

    @MockBean
    private AuthTokenGenerator authTokenGenerator;

    @MockBean
    private OcrValidationClient ocrValidationClient;

    private OcrValidator ocrValidator;

    private BlobContainerClient testContainer;

    private static DockerComposeContainer dockerComposeContainer;
    private static String dockerHost;

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

    @Test
    void should_process_envelope_without_warnings_if_ocr_validation_returns_server_side_error_and_then_passes()
        throws Exception {
        // given
        uploadToBlobStorage(SAMPLE_ZIP_FILE_NAME, zipDir("zipcontents/supplementary_evidence_with_ocr"));

        given(authTokenGenerator.generate()).willReturn("token");
        given(ocrValidationClient.validate(anyString(), any(FormData.class), anyString(), anyString()))
            .willThrow(getServerSideException())
            .willReturn(new ValidationResponse(SUCCESS, emptyList(), emptyList()))
        ;

        // when
        processor.processBlobs();

        retryAfterDelay();

        // then
        Envelope actualEnvelope = getSingleEnvelopeFromDb();

        String originalMetaFile = Resources.toString(
            getResource("zipcontents/supplementary_evidence_with_ocr/metadata.json"),
            Charset.defaultCharset()
        );

        assertThat(parse(actualEnvelope)).isEqualToIgnoringGivenFields(
            parse(originalMetaFile),
            "id", "amount", "amount_in_pence", "configuration", "json"
        );
        assertThat(actualEnvelope.getStatus()).isEqualTo(CREATED);
        assertThat(actualEnvelope.getScannableItems()).hasSize(1);
        assertThat(actualEnvelope.getScannableItems().get(0).getOcrValidationWarnings().length).isEqualTo(0);

        // and
        List<ProcessEvent> processEvents = processEventRepository.findAll();
        assertThat(processEvents.stream().map(ProcessEvent::getEvent).collect(toList()))
            .containsExactlyInAnyOrder(
                ZIPFILE_PROCESSING_STARTED,
                ZIPFILE_PROCESSING_STARTED
            );

        assertThat(processEvents).allMatch(pe -> pe.getReason() == null);
    }

    @NotNull
    private HttpServerErrorException getServerSideException() {
        return HttpServerErrorException.create(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "internal server error message",
            HttpHeaders.EMPTY,
            null,
            null
        );
    }

    private void uploadToBlobStorage(String fileName, byte[] fileContent) {
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

    private Envelope getSingleEnvelopeFromDb() {
        // We expect only one envelope which was uploaded
        List<Envelope> envelopes = envelopeRepository.findAll();
        assertThat(envelopes).hasSize(1);

        return envelopes.get(0);
    }

    private void assertNoEnvelopesInDb() {
        // We expect only one envelope which was uploaded
        List<Envelope> envelopes = envelopeRepository.findAll();
        assertThat(envelopes).hasSize(0);
    }

    private void doSleep(long l) {
        try {
            Thread.sleep(l);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    private void retryAfterDelay() {
        assertNoEnvelopesInDb();

        doSleep(5000L);

        processor.processBlobs();
    }
//
//    @TestConfiguration
//    public static class MockConfig {
//
//        @Bean(name = "notifications-helper")
//        public ServiceBusHelper notificationsQueueHelper() {
//            return mock(ServiceBusHelper.class);
//        }
//    }
}
