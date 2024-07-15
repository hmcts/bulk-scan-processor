package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;
import uk.gov.hmcts.reform.bulkscanprocessor.config.BlobManagementProperties;
import uk.gov.hmcts.reform.bulkscanprocessor.config.ContainerMappings;
import uk.gov.hmcts.reform.bulkscanprocessor.config.IntegrationContextInitializer;
import uk.gov.hmcts.reform.bulkscanprocessor.config.IntegrationTest;
import uk.gov.hmcts.reform.bulkscanprocessor.config.Profiles;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.helper.DirectoryZipper;
import uk.gov.hmcts.reform.bulkscanprocessor.services.EnvelopeHandler;
import uk.gov.hmcts.reform.bulkscanprocessor.services.FileContentProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.services.FileRejector;
import uk.gov.hmcts.reform.bulkscanprocessor.services.UploadEnvelopeDocumentsService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.DocumentManagementService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.storage.LeaseAcquirer;
import uk.gov.hmcts.reform.bulkscanprocessor.services.storage.LeaseMetaDataChecker;
import uk.gov.hmcts.reform.bulkscanprocessor.services.storage.OcrValidationRetryManager;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.BlobProcessorTask;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.UploadEnvelopeDocumentsTask;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.BlobManager;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.DocumentProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.ZipFileProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.util.TestStorageHelper;
import uk.gov.hmcts.reform.bulkscanprocessor.validation.EnvelopeValidator;
import uk.gov.hmcts.reform.bulkscanprocessor.validation.MetafileJsonValidator;
import uk.gov.hmcts.reform.bulkscanprocessor.validation.OcrValidator;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.List;

import static com.google.common.io.Resources.getResource;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.bulkscanprocessor.util.AzureHelper.AZURE_TEST_CONTAINER;
import static uk.gov.hmcts.reform.bulkscanprocessor.util.AzureHelper.CONTAINER_PORT;
import static uk.gov.hmcts.reform.bulkscanprocessor.util.AzureHelper.EXTRACTION_HOST;

@ActiveProfiles({
    IntegrationContextInitializer.PROFILE_WIREMOCK,
    Profiles.SERVICE_BUS_STUB,
    Profiles.STORAGE_STUB
})
@AutoConfigureMockMvc
@IntegrationTest
@SuppressWarnings("unchecked")
class ZipStatusControllerTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private MetafileJsonValidator schemaValidator;
    @Autowired private ContainerMappings containerMappings;
    @Autowired private ZipFileProcessor zipFileProcessor;
    @Autowired private EnvelopeRepository envelopeRepository;
    @Autowired private ProcessEventRepository processEventRepository;
    @Autowired private BlobManagementProperties blobManagementProperties;
    @Autowired private LeaseMetaDataChecker leaseMetaDataChecker;
    @Autowired private DocumentProcessor documentProcessor;
    @Autowired private LeaseAcquirer leaseAcquirer;
    @Autowired private OcrValidationRetryManager ocrValidationRetryManager;
    @Autowired private EnvelopeProcessor envelopeProcessor;
    @Autowired private OcrValidator ocrValidator;

    @Value("${process-payments.enabled}") private boolean paymentsEnabled;

    @MockBean private DocumentManagementService documentManagementService;
    @MockBean private AuthTokenValidator tokenValidator;
    @MockBean private FileRejector fileRejector;

    private BlobProcessorTask blobProcessorTask;
    private UploadEnvelopeDocumentsTask uploadTask;
    private BlobContainerClient testContainer;


    private static GenericContainer<?> dockerComposeContainer =
        new GenericContainer<>(AZURE_TEST_CONTAINER).withExposedPorts(CONTAINER_PORT);

    private static String dockerHost;

    @BeforeAll
    public static void initialize() {
        dockerComposeContainer.withEnv("executable", "blob");
        dockerComposeContainer.withNetworkAliases(EXTRACTION_HOST);
        dockerComposeContainer.start();
        dockerHost = dockerComposeContainer.getHost();
    }

    @AfterAll
    static void tearDownContainer() {
        dockerComposeContainer.stop();
    }

    @BeforeEach
    void setup() {

        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                .connectionString(String.format(TestStorageHelper.STORAGE_CONN_STRING, dockerHost, 10000))
                .buildClient();

        BlobManager blobManager = new BlobManager(blobServiceClient, blobManagementProperties);
        EnvelopeValidator envelopeValidator = new EnvelopeValidator();
        EnvelopeHandler envelopeHandler = new EnvelopeHandler(
            envelopeValidator,
            containerMappings,
            envelopeProcessor,
            ocrValidator,
            paymentsEnabled
        );

        FileContentProcessor fileContentProcessor = new FileContentProcessor(
            zipFileProcessor,
            envelopeProcessor,
            envelopeHandler,
            fileRejector
        );

        blobProcessorTask = new BlobProcessorTask(
            blobManager,
            envelopeProcessor,
            fileContentProcessor,
            leaseAcquirer,
            ocrValidationRetryManager
        );

        UploadEnvelopeDocumentsService uploadService =  new UploadEnvelopeDocumentsService(
            blobManager,
            zipFileProcessor,
            documentProcessor,
            envelopeProcessor,
            leaseAcquirer
        );

        uploadTask = new UploadEnvelopeDocumentsTask(envelopeRepository, uploadService, 1);

        testContainer = blobServiceClient.getBlobContainerClient("bulkscan");
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

    @Test
    void should_successfully_return_zip_status()
        throws Exception {

        uploadZipToBlobStore("zipcontents/all_items", "1_24-06-2018-00-00-00.zip");

        given(documentManagementService.uploadDocuments(any(), anyString(), anyString()))
            .willReturn(
                ImmutableMap.of("1111002.pdf", "http://localhost:8080/documents/0fa1ab60-f836-43aa-8c65-b07cc9bebcbe")
            );

        blobProcessorTask.processBlobs();
        uploadTask.run();

        given(tokenValidator.getServiceName("testServiceAuthHeader")).willReturn("test_service");

        mockMvc.perform(get("/zip-files?name=" + "1_24-06-2018-00-00-00.zip")
            .header("ServiceAuthorization", "testServiceAuthHeader"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(APPLICATION_JSON_VALUE))
            .andExpect(content().json(Resources.toString(getResource("zipstatus.json"), UTF_8)));

        ArgumentCaptor<List<File>> pdfListCaptor = ArgumentCaptor.forClass(List.class);
        verify(documentManagementService,times(1))
            .uploadDocuments(pdfListCaptor.capture(), eq("BULKSCAN"), eq("bulkscan"));
        assertThat(pdfListCaptor.getValue().get(0).getName()).isEqualTo("1111002.pdf");
    }

    private void uploadZipToBlobStore(String dirToZip, String zipFilename) throws Exception {
        byte[] zipFile = DirectoryZipper.zipDir(dirToZip);

        testContainer
            .getBlobClient(zipFilename)
            .upload(new ByteArrayInputStream(zipFile), zipFile.length);
    }
}
