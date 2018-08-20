package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import org.apache.commons.io.Charsets;
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
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItemRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ServiceJuridictionConfigNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.UnAuthenticatedException;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.DocumentManagementService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.output.Pdf;
import uk.gov.hmcts.reform.bulkscanprocessor.services.wrapper.ErrorHandlingWrapper;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.BlobProcessorTask;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.DocumentProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import static com.google.common.io.Resources.getResource;
import static com.google.common.io.Resources.toByteArray;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.METADATA_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.PROCESSED;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class EnvelopeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String DOCUMENT_URL = "http://localhost:8080/documents/0fa1ab60-f836-43aa-8c65-b07cc9bebcbe";


    private CloudBlobClient cloudBlobClient;

    private BlobProcessorTask blobProcessorTask;

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

    @Mock
    private DocumentManagementService documentManagementService;

    @MockBean
    private AuthTokenValidator tokenValidator;

    private DocumentProcessor documentProcessor;

    private EnvelopeProcessor envelopeProcessor;

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
        cloudBlobClient = account.createCloudBlobClient();

        documentProcessor = new DocumentProcessor(
            documentManagementService,
            scannableItemRepository
        );

        envelopeProcessor = new EnvelopeProcessor(
            envelopeRepository,
            processEventRepository,
            reUploadBatchSize
        );

        blobProcessorTask = new BlobProcessorTask(
            cloudBlobClient,
            documentProcessor,
            envelopeProcessor,
            errorWrapper
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
        uploadZipToBlobStore("7_24-06-2018-00-00-00.zip"); //Zip file with metadata and pdf
        uploadZipToBlobStore("8_24-06-2018-00-00-00.zip"); // Zip file with metadata and pdf

        byte[] testPdfBytes = toByteArray(getResource("1111002.pdf"));
        Pdf pdf = new Pdf("1111002.pdf", testPdfBytes);

        given(documentManagementService.uploadDocuments(ImmutableList.of(pdf)))
            .willReturn(ImmutableMap.of("1111002.pdf", DOCUMENT_URL));

        blobProcessorTask.processBlobs();

        given(tokenValidator.getServiceName("testServiceAuthHeader")).willReturn("test_service");

        mockMvc.perform(get("/envelopes?status=" + PROCESSED)
            .header("ServiceAuthorization", "testServiceAuthHeader"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json;charset=UTF-8"))
            .andExpect(content().json(expectedEnvelopes()))
            // Envelope id is checked explicitly as it is dynamically generated.
            .andExpect(MockMvcResultMatchers.jsonPath("envelopes[0].id").exists());

        assertThat(envelopeRepository.findAll())
            .hasSize(2)
            .extracting("zipFileName", "status")
            .containsExactlyInAnyOrder(
                tuple("7_24-06-2018-00-00-00.zip", PROCESSED),
                tuple("8_24-06-2018-00-00-00.zip", METADATA_FAILURE)
            );

        byte[] testPdfBytes1 = toByteArray(getResource("1111005.pdf"));
        Pdf pdf1 = new Pdf("1111005.pdf", testPdfBytes1);

        verify(documentManagementService, never()).uploadDocuments(ImmutableList.of(pdf1));
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

    private String expectedEnvelopes() throws IOException {
        URL url = getResource("envelope.json");
        return Resources.toString(url, Charsets.toCharset("UTF-8"));
    }

    private void uploadZipToBlobStore(String fileName) throws Exception {
        byte[] zipFile = toByteArray(getResource(fileName));

        CloudBlockBlob blockBlobReference = testContainer.getBlockBlobReference(fileName);
        blockBlobReference.uploadFromByteArray(zipFile, 0, zipFile.length);
    }
}
