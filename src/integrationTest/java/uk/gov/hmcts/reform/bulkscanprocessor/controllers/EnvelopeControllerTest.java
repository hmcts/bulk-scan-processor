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
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.testcontainers.containers.DockerComposeContainer;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItemRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ServiceJuridictionConfigNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.UnAuthenticatedException;
import uk.gov.hmcts.reform.bulkscanprocessor.helpers.DirectoryZipper;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.DocumentManagementService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.output.Pdf;
import uk.gov.hmcts.reform.bulkscanprocessor.services.wrapper.ErrorHandlingWrapper;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.BlobProcessorTask;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.DocumentProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.validation.MetafileJsonValidator;

import java.io.File;
import java.util.List;
import java.util.UUID;

import static com.google.common.io.Resources.getResource;
import static com.google.common.io.Resources.toByteArray;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.PROCESSED;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class EnvelopeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private BlobProcessorTask blobProcessorTask;

    @Autowired
    private MetafileJsonValidator schemaValidator;

    @Autowired
    private EnvelopeRepository envelopeRepository;

    @Autowired
    private ProcessEventRepository processEventRepository;

    @Autowired
    private ScannableItemRepository scannableItemRepository;

    @Autowired
    private ErrorHandlingWrapper errorWrapper;

    @Value("${scheduling.task.reupload.batch}")
    private int reUploadBatchSize;

    @Value("${scheduling.task.reupload.max_tries}")
    private int reuploadMaxTries;

    @Mock
    private DocumentManagementService documentManagementService;

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

        blobProcessorTask = new BlobProcessorTask(
            cloudBlobClient,
            new DocumentProcessor(
                documentManagementService,
                scannableItemRepository
            ),
            new EnvelopeProcessor(
                schemaValidator,
                envelopeRepository,
                processEventRepository,
                reUploadBatchSize,
                reuploadMaxTries
            ),
            errorWrapper,
            "none",
            "none"

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
    public void should_successfully_return_all_envelopes_with_processed_status_for_a_given_jurisdiction()
        throws Exception {

        uploadZipToBlobStore("zipcontents/ok");
        uploadZipToBlobStore("zipcontents/mismatching_pdfs");

        Pdf okPdf = new Pdf("1111002.pdf", toByteArray(getResource("zipcontents/ok/1111002.pdf")));

        given(documentManagementService.uploadDocuments(ImmutableList.of(okPdf)))
            .willReturn(
                ImmutableMap.of("1111002.pdf", "http://localhost:8080/documents/0fa1ab60-f836-43aa-8c65-b07cc9bebcbe")
            );

        blobProcessorTask.processBlobs();

        given(tokenValidator.getServiceName("testServiceAuthHeader")).willReturn("test_service");

        mockMvc.perform(get("/envelopes?status=" + PROCESSED)
            .header("ServiceAuthorization", "testServiceAuthHeader"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json;charset=UTF-8"))
            .andExpect(content().json(Resources.toString(getResource("envelope.json"), UTF_8)))
            // Envelope id is checked explicitly as it is dynamically generated.
            .andExpect(MockMvcResultMatchers.jsonPath("envelopes[0].id").exists());

        List<Envelope> envelopes = envelopeRepository.findAll();
        assertThat(envelopes).hasSize(1);
        assertThat(envelopes.get(0).getStatus()).isEqualTo(PROCESSED);

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

    private void uploadZipToBlobStore(String dirToZip) throws Exception {
        byte[] zipFile = DirectoryZipper.zipDir(dirToZip);

        testContainer
            .getBlockBlobReference(UUID.randomUUID() + "_01-01-2018-00-00-00.zip")
            .uploadFromByteArray(zipFile, 0, zipFile.length);
    }
}
