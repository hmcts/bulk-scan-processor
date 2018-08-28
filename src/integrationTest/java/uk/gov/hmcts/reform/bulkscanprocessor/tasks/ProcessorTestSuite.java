package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import org.json.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.testcontainers.containers.DockerComposeContainer;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItemRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.DocumentManagementService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.output.Pdf;
import uk.gov.hmcts.reform.bulkscanprocessor.services.wrapper.ErrorHandlingWrapper;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.DocumentProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static com.google.common.io.Resources.getResource;
import static com.google.common.io.Resources.toByteArray;

public abstract class ProcessorTestSuite<T extends Processor> {

    protected static final String ZIP_FILE_NAME_SUCCESS = "1_24-06-2018-00-00-00.zip";

    protected static final String DOCUMENT_URL1 =
        "http://localhost:8080/documents/1971cadc-9f79-4e1d-9033-84543bbbbc1d";
    protected static final String DOCUMENT_URL2 =
        "http://localhost:8080/documents/0fa1ab60-f836-43aa-8c65-b07cc9bebcbe";

    @FunctionalInterface
    public interface Construct<T extends Processor> {
        T apply(
            CloudBlobClient cloudBlobClient,
            DocumentProcessor documentProcessor,
            EnvelopeProcessor envelopeProcessor,
            ErrorHandlingWrapper errorWrapper
        );
    }

    protected T processor;

    @Autowired
    private Consumer<JSONObject> jsonValidator;

    @Autowired
    protected EnvelopeRepository envelopeRepository;

    @Autowired
    protected ProcessEventRepository processEventRepository;

    protected DocumentProcessor documentProcessor;

    protected EnvelopeProcessor envelopeProcessor;

    @Autowired
    protected ErrorHandlingWrapper errorWrapper;

    @Autowired
    private ScannableItemRepository scannableItemRepository;

    @Value("${scheduling.task.reupload.batch}")
    private int reUploadBatchSize;

    @Value("${scheduling.task.reupload.max_tries}")
    private int reuploadMaxTries;

    @Mock
    protected DocumentManagementService documentManagementService;

    protected CloudBlobContainer testContainer;

    private static DockerComposeContainer dockerComposeContainer;

    public void setUp(Construct<T> processorConstruct) throws Exception {

        CloudStorageAccount account = CloudStorageAccount.parse("UseDevelopmentStorage=true");
        CloudBlobClient cloudBlobClient = account.createCloudBlobClient();

        documentProcessor = new DocumentProcessor(
            documentManagementService,
            scannableItemRepository
        );

        envelopeProcessor = new EnvelopeProcessor(
            jsonValidator,
            envelopeRepository,
            processEventRepository,
            reUploadBatchSize,
            reuploadMaxTries
        );

        processor = processorConstruct.apply(
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

    protected void uploadZipToBlobStore(String zipFileName) throws Exception {
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

    protected Map<String, String> getFileUploadResponse() {
        return ImmutableMap.of(
            "1111001.pdf", DOCUMENT_URL1,
            "1111002.pdf", DOCUMENT_URL2
        );
    }

}
