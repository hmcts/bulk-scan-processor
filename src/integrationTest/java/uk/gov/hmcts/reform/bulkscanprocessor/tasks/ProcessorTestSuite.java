package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.palantir.docker.compose.DockerComposeRule;
import com.palantir.docker.compose.connection.waiting.HealthChecks;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItemRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.DocumentManagementService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.output.Pdf;
import uk.gov.hmcts.reform.bulkscanprocessor.services.wrapper.ErrorHandlingWrapper;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.DocumentProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.google.common.io.Resources.getResource;
import static com.google.common.io.Resources.toByteArray;

public abstract class ProcessorTestSuite {

    static final String ZIP_FILE_NAME_SUCCESS = "1_24-06-2018-00-00-00.zip";

    static final String DOCUMENT_URL1 = "http://localhost:8080/documents/1971cadc-9f79-4e1d-9033-84543bbbbc1d";
    static final String DOCUMENT_URL2 = "http://localhost:8080/documents/0fa1ab60-f836-43aa-8c65-b07cc9bebcbe";

    @ClassRule
    public static DockerComposeRule docker = DockerComposeRule.builder()
        .file("src/integrationTest/resources/docker-compose.yml")
        .waitingForService("azure-storage", HealthChecks.toHaveAllPortsOpen())
        .waitingForService("azure-storage", HealthChecks.toRespondOverHttp(10000, (port) -> port.inFormat("http://$HOST:$EXTERNAL_PORT/devstoreaccount1?comp=list")))
        .build();

    BlobProcessorTask blobProcessorTask;

    @Autowired
    EnvelopeRepository envelopeRepository;

    @Autowired
    ProcessEventRepository processEventRepository;

    @Autowired
    private ErrorHandlingWrapper errorWrapper;

    @Autowired
    private ScannableItemRepository scannableItemRepository;

    @Value("${scheduling.task.reupload.batch}")
    private int reUploadBatchSize;

    @Mock
    DocumentManagementService documentManagementService;

    CloudBlobContainer testContainer;

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

    void uploadZipToBlobStore(String zipFileName) throws Exception {
        byte[] zipFile = toByteArray(getResource(zipFileName));

        CloudBlockBlob blockBlobReference = testContainer.getBlockBlobReference(zipFileName);
        blockBlobReference.uploadFromByteArray(zipFile, 0, zipFile.length);
    }

    List<Pdf> getUploadResources() throws IOException {
        byte[] test1PdfBytes = toByteArray(getResource("1111001.pdf"));
        byte[] test2PdfBytes = toByteArray(getResource("1111002.pdf"));

        Pdf pdf1 = new Pdf("1111001.pdf", test1PdfBytes);
        Pdf pdf2 = new Pdf("1111002.pdf", test2PdfBytes);

        return ImmutableList.of(pdf1, pdf2);
    }

    Map<String, String> getFileUploadResponse() {
        return ImmutableMap.of(
            "1111001.pdf", DOCUMENT_URL1,
            "1111002.pdf", DOCUMENT_URL2
        );
    }

}
