package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.testcontainers.containers.DockerComposeContainer;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;
import uk.gov.hmcts.reform.bulkscanprocessor.config.BlobManagementProperties;
import uk.gov.hmcts.reform.bulkscanprocessor.config.ContainerMappings;
import uk.gov.hmcts.reform.bulkscanprocessor.config.IntegrationContextInitializer;
import uk.gov.hmcts.reform.bulkscanprocessor.config.IntegrationTest;
import uk.gov.hmcts.reform.bulkscanprocessor.config.Profiles;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItemRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ServiceJuridictionConfigNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.UnAuthenticatedException;
import uk.gov.hmcts.reform.bulkscanprocessor.helper.DirectoryZipper;
import uk.gov.hmcts.reform.bulkscanprocessor.services.UploadEnvelopeDocumentsService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.DocumentManagementService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.output.Pdf;
import uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.ServiceBusHelper;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.BlobProcessorTask;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.UploadEnvelopeDocumentsTask;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.BlobManager;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.DocumentProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.ZipFileProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.validation.MetafileJsonValidator;
import uk.gov.hmcts.reform.bulkscanprocessor.validation.OcrValidator;

import java.io.File;
import java.util.List;

import static com.google.common.io.Resources.getResource;
import static com.google.common.io.Resources.toByteArray;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.UPLOADED;

@ActiveProfiles({
    IntegrationContextInitializer.PROFILE_WIREMOCK,
    Profiles.SERVICE_BUS_STUB,
    Profiles.STORAGE_STUB
})
@AutoConfigureMockMvc
@IntegrationTest
@RunWith(SpringRunner.class)
public class EnvelopeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private BlobProcessorTask blobProcessorTask;

    private UploadEnvelopeDocumentsTask uploadTask;

    @Autowired
    private MetafileJsonValidator schemaValidator;

    @Autowired
    private ContainerMappings containerMappings;

    @Autowired
    private ZipFileProcessor zipFileProcessor;

    @Autowired
    private EnvelopeRepository envelopeRepository;

    @Autowired
    private ProcessEventRepository processEventRepository;

    @Autowired
    private ScannableItemRepository scannableItemRepository;

    @Value("${process-payments.enabled}")
    private boolean paymentsEnabled;

    @MockBean
    private DocumentManagementService documentManagementService;

    @MockBean
    private OcrValidator ocrValidator;

    @MockBean
    private ServiceBusHelper serviceBusHelper;

    @Autowired
    private BlobManagementProperties blobManagementProperties;

    @Autowired
    private UploadEnvelopeDocumentsService uploadService;

    @MockBean
    private AuthTokenValidator tokenValidator;

    private CloudBlobContainer testContainer;

    private static DockerComposeContainer dockerComposeContainer;

    @BeforeClass
    public static void initialize() {
        File dockerComposeFile = new File("src/integrationTest/resources/docker-compose.yml");

        dockerComposeContainer = new DockerComposeContainer(dockerComposeFile)
            .withExposedService("azure-storage", 10000);

        dockerComposeContainer.start();
    }

    @AfterClass
    public static void tearDownContainer() {
        dockerComposeContainer.stop();
    }

    @Before
    public void setup() throws Exception {
        CloudStorageAccount account = CloudStorageAccount.parse("UseDevelopmentStorage=true");
        CloudBlobClient cloudBlobClient = account.createCloudBlobClient();
        BlobManager blobManager = new BlobManager(cloudBlobClient, blobManagementProperties);

        blobProcessorTask = new BlobProcessorTask(
            blobManager,
            new DocumentProcessor(
                documentManagementService,
                scannableItemRepository
            ),
            new EnvelopeProcessor(
                schemaValidator,
                envelopeRepository,
                processEventRepository
            ),
            zipFileProcessor,
            envelopeRepository,
            processEventRepository,
            containerMappings,
            ocrValidator,
            serviceBusHelper,
            paymentsEnabled
        );
        uploadTask = new UploadEnvelopeDocumentsTask(envelopeRepository, uploadService);

        testContainer = cloudBlobClient.getContainerReference("bulkscan");
        testContainer.createIfNotExists();
    }

    @After
    public void cleanUp() throws Exception {
        testContainer.deleteIfExists();
        envelopeRepository.deleteAll();
        processEventRepository.deleteAll();
    }

    @Test
    public void should_successfully_return_all_envelopes_with_processed_status_for_a_given_jurisdiction()
        throws Exception {

        uploadZipToBlobStore("zipcontents/ok", "1_24-06-2018-00-00-00.zip");
        uploadZipToBlobStore("zipcontents/mismatching_pdfs", "8_24-06-2018-00-00-00.zip");

        Pdf okPdf = new Pdf("1111002.pdf", toByteArray(getResource("zipcontents/ok/1111002.pdf")));

        given(documentManagementService.uploadDocuments(ImmutableList.of(okPdf)))
            .willReturn(
                ImmutableMap.of("1111002.pdf", "http://localhost:8080/documents/0fa1ab60-f836-43aa-8c65-b07cc9bebcbe")
            );

        blobProcessorTask.processBlobs();
        uploadTask.run();

        given(tokenValidator.getServiceName("testServiceAuthHeader")).willReturn("test_service");

        mockMvc.perform(get("/envelopes?status=" + UPLOADED)
            .header("ServiceAuthorization", "testServiceAuthHeader"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(APPLICATION_JSON_VALUE))
            .andExpect(content().json(Resources.toString(getResource("envelope.json"), UTF_8)))
            // Envelope id is checked explicitly as it is dynamically generated.
            .andExpect(MockMvcResultMatchers.jsonPath("envelopes[0].id").exists());

        List<Envelope> envelopes = envelopeRepository.findAll();
        assertThat(envelopes).hasSize(1);
        assertThat(envelopes.get(0).getStatus()).isEqualTo(UPLOADED);

        verify(documentManagementService, never())
            .uploadDocuments(
                ImmutableList.of(
                    new Pdf("1111005.pdf", toByteArray(getResource("zipcontents/mismatching_pdfs/1111005.pdf")))
                )
            );
        verify(tokenValidator).getServiceName("testServiceAuthHeader");
    }

    @Test
    public void should_return_empty_list_when_no_envelopes_are_available() throws Exception {
        given(tokenValidator.getServiceName("testServiceAuthHeader")).willReturn("test_service");

        mockMvc.perform(get("/envelopes")
            .header("ServiceAuthorization", "testServiceAuthHeader"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().string("{\"envelopes\":[]}"));

        verify(tokenValidator).getServiceName("testServiceAuthHeader");
    }

    @Test
    public void should_throw_service_jurisdiction_config_not_found_exc_when_service_jurisdiction_mapping_is_not_found()
        throws Exception {
        given(tokenValidator.getServiceName("testServiceAuthHeader")).willReturn("test");

        MvcResult result = this.mockMvc.perform(get("/envelopes")
            .header("ServiceAuthorization", "testServiceAuthHeader"))
            .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(400);

        assertThat(result.getResolvedException()).isInstanceOf(ServiceJuridictionConfigNotFoundException.class);

        verify(tokenValidator).getServiceName("testServiceAuthHeader");
    }

    @Test
    public void should_throw_unauthenticated_exception_when_service_auth_header_is_missing() throws Exception {
        given(tokenValidator.getServiceName("testServiceAuthHeader")).willThrow(UnAuthenticatedException.class);

        MvcResult result = this.mockMvc.perform(get("/envelopes")).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(401);

        assertThat(result.getResolvedException()).isInstanceOf(UnAuthenticatedException.class);
    }

    private void uploadZipToBlobStore(String dirToZip, String zipFilename) throws Exception {
        byte[] zipFile = DirectoryZipper.zipDir(dirToZip);

        testContainer
            .getBlockBlobReference(zipFilename)
            .uploadFromByteArray(zipFile, 0, zipFile.length);
    }
}
