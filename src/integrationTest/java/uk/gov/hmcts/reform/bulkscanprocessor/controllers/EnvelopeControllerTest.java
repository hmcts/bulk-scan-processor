package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.palantir.docker.compose.DockerComposeRule;
import org.apache.commons.io.Charsets;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeStateRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItemRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.DocumentManagementService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.output.Pdf;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.BlobProcessorTask;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.DocumentProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;

import java.io.IOException;
import java.net.URL;

import static com.google.common.io.Resources.getResource;
import static com.google.common.io.Resources.toByteArray;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class EnvelopeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String DOCUMENT_URL = "http://localhost:8080/documents/0fa1ab60-f836-43aa-8c65-b07cc9bebcbe";

    @ClassRule
    public static DockerComposeRule docker = DockerComposeRule.builder()
        .file("src/integrationTest/resources/docker-compose.yml")
        .build();

    private CloudBlobClient cloudBlobClient;

    private BlobProcessorTask blobProcessorTask;

    @Autowired
    private EnvelopeRepository envelopeRepository;

    @Autowired
    private EnvelopeStateRepository envelopeStateRepository;

    @Autowired
    private ScannableItemRepository scannableItemRepository;

    @Mock
    private DocumentManagementService documentManagementService;

    private DocumentProcessor documentProcessor;

    private EnvelopeProcessor envelopeProcessor;

    private CloudBlobContainer testContainer;

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
            envelopeStateRepository
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
    }

    @Test
    public void should_successfully_return_all_envelopes() throws Exception {
        uploadZipToBlobStore("7_24-06-2018-00-00-00.zip"); //Zip file with metadata and pdf

        byte[] testPdfBytes = toByteArray(getResource("1111002.pdf"));

        Pdf pdf = new Pdf("1111002.pdf", testPdfBytes);

        given(documentManagementService.uploadDocuments(ImmutableList.of(pdf)))
            .willReturn(ImmutableMap.of(
                "1111002.pdf", DOCUMENT_URL)
            );

        blobProcessorTask.processBlobs();

        mockMvc.perform(get("/envelopes"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().json(expectedEnvelopes()));

    }

    @Test
    public void should_return_empty_list_when_envelopes_are_available() throws Exception {
        mockMvc.perform(get("/envelopes"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().string("{\"envelopes\":[]}"));

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
