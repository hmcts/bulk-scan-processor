package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.testcontainers.containers.DockerComposeContainer;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItemRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.DocumentManagementService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.ServiceBusHelper;
import uk.gov.hmcts.reform.bulkscanprocessor.services.wrapper.ErrorHandlingWrapper;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.DocumentProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.validation.MetafileJsonValidator;

import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.List;

import static com.google.common.io.Resources.getResource;
import static com.google.common.io.Resources.toByteArray;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

public abstract class ProcessorTestSuite<T extends Processor> {

    protected static final String SAMPLE_ZIP_FILE_NAME = "hello_24-06-2018-00-00-00.zip";

    protected static final String VALID_ZIP_FILE_WITH_CASE_NUMBER = "1_24-06-2018-00-00-00.zip";

    protected static final String DOCUMENT_URL1 =
        "http://localhost:8080/documents/1971cadc-9f79-4e1d-9033-84543bbbbc1d";
    protected static final String DOCUMENT_URL2 =
        "http://localhost:8080/documents/0fa1ab60-f836-43aa-8c65-b07cc9bebcbe";

    protected static final String SIGNATURE_ALGORITHM = "none";
    protected static final String PUBLIC_KEY_BASE64 = "none";

    @FunctionalInterface
    public interface Construct<T extends Processor> {
        T apply(
            CloudBlobClient cloudBlobClient,
            DocumentProcessor documentProcessor,
            EnvelopeProcessor envelopeProcessor,
            ErrorHandlingWrapper errorWrapper,
            String signatureAlg,
            String publicKeyBase64
        );
    }

    protected T processor;

    @Autowired
    private MetafileJsonValidator schemaValidator;

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

    @Mock
    protected ServiceBusHelper serviceBusHelper;

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
            schemaValidator,
            envelopeRepository,
            processEventRepository,
            reUploadBatchSize,
            reuploadMaxTries
        );

        T p = processorConstruct.apply(
            cloudBlobClient,
            documentProcessor,
            envelopeProcessor,
            errorWrapper,
            SIGNATURE_ALGORITHM,
            PUBLIC_KEY_BASE64
        );

        processor = spy(p);
        doReturn(serviceBusHelper).when(processor).serviceBusHelper();
        
        testContainer = cloudBlobClient.getContainerReference("test");
        testContainer.createIfNotExists();
    }

    @After
    public void cleanUp() throws Exception {
        testContainer.deleteIfExists();
        envelopeRepository.deleteAll();
        processEventRepository.deleteAll();
    }

    @Before
    public void prepare() throws Exception {
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
        uploadToBlobStorage(zipFileName, toByteArray(getResource(zipFileName)));
    }

    public void uploadToBlobStorage(String fileName, byte[] fileContent) throws Exception {
        CloudBlockBlob blockBlobReference = testContainer.getBlockBlobReference(fileName);

        // Blob need to be deleted as same blob may exists if previously uploaded blob was not deleted
        // due to doc upload failure
        if (blockBlobReference.exists()) {
            blockBlobReference.breakLease(0);
            blockBlobReference.delete();
        }

        // A Put Blob operation may succeed against a blob that exists in the storage emulator with an active lease,
        // even if the lease ID has not been specified in the request.
        blockBlobReference.uploadFromByteArray(fileContent, 0, fileContent.length);
    }

    // TODO: add repo method to read single envelope by zip file name and jurisdiction.
    protected Envelope getSingleEnvelopeFromDb() {
        // We expect only one envelope which was uploaded
        List<Envelope> envelopes = envelopeRepository.findAll();
        assertThat(envelopes).hasSize(1);

        return envelopes.get(0);
    }

    protected String getXyzPublicKey64() throws IOException {
        byte[] xyzPublicKeyBytes = toByteArray(getResource("public_key.der"));
        return Base64.getEncoder().encodeToString(xyzPublicKeyBytes);
    }

}
