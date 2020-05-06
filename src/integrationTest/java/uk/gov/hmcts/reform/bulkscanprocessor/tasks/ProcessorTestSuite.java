package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.testcontainers.containers.DockerComposeContainer;
import uk.gov.hmcts.reform.bulkscanprocessor.config.BlobManagementProperties;
import uk.gov.hmcts.reform.bulkscanprocessor.config.ContainerMappings;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItemRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorCode;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorMsg;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.DocumentManagementService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.ServiceBusHelper;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.BlobManager;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.DocumentProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.ZipFileProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.validation.MetafileJsonValidator;
import uk.gov.hmcts.reform.bulkscanprocessor.validation.OcrValidator;

import java.io.File;
import java.util.List;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;

public abstract class ProcessorTestSuite<T extends Processor> {

    protected static final String SAMPLE_ZIP_FILE_NAME = "1_24-06-2018-00-00-00.zip";

    protected static final String DOCUMENT_URL1 =
        "http://localhost:8080/documents/1971cadc-9f79-4e1d-9033-84543bbbbc1d";
    protected static final String DOCUMENT_URL2 =
        "http://localhost:8080/documents/0fa1ab60-f836-43aa-8c65-b07cc9bebcbe";
    protected static final String DOCUMENT_UUID2 = "0fa1ab60-f836-43aa-8c65-b07cc9bebcbe";

    protected static final String CONTAINER_NAME = "bulkscan";
    protected static final String PO_BOX = "BULKSCANPO";
    protected static final String REJECTED_CONTAINER_NAME = "bulkscan-rejected";

    protected T processor;

    protected static final int RETRY_COUNT = 2;

    @Autowired
    protected ZipFileProcessor zipFileProcessor;

    @Autowired
    private MetafileJsonValidator schemaValidator;

    @Autowired
    protected ContainerMappings containerMappings;

    @Autowired
    protected EnvelopeRepository envelopeRepository;

    @Autowired
    protected ProcessEventRepository processEventRepository;

    protected DocumentProcessor documentProcessor;

    protected EnvelopeProcessor envelopeProcessor;

    protected BlobManager blobManager;

    @Autowired
    private ScannableItemRepository scannableItemRepository;

    @Autowired
    private BlobManagementProperties blobManagementProperties;

    @Value("${scheduling.task.reupload.batch}")
    private int reUploadBatchSize;

    @Value("${scheduling.task.reupload.max_tries}")
    private int reuploadMaxTries;

    @Mock
    protected DocumentManagementService documentManagementService;

    @Mock
    protected OcrValidator ocrValidator;

    @Mock
    protected ServiceBusHelper serviceBusHelper;

    @Value("${process-payments.enabled}")
    protected boolean paymentsEnabled;

    protected CloudBlobContainer testContainer;
    protected CloudBlobContainer rejectedContainer;

    private static DockerComposeContainer dockerComposeContainer;

    public void setUp() throws Exception {

        CloudStorageAccount account = CloudStorageAccount.parse("UseDevelopmentStorage=true");
        CloudBlobClient cloudBlobClient = account.createCloudBlobClient();

        blobManager = new BlobManager(cloudBlobClient, blobManagementProperties);

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

        testContainer = cloudBlobClient.getContainerReference(CONTAINER_NAME);
        testContainer.createIfNotExists();

        rejectedContainer = cloudBlobClient.getContainerReference(REJECTED_CONTAINER_NAME);
        rejectedContainer.createIfNotExists();
    }

    @After
    public void cleanUp() throws Exception {
        testContainer.deleteIfExists();
        rejectedContainer.deleteIfExists();
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

    protected void eventsWereCreated(Event event1, Event event2) {
        assertThat(processEventRepository.findAll())
            .hasSize(2)
            .extracting(e -> tuple(e.getContainer(), e.getEvent()))
            .containsExactlyInAnyOrder(
                tuple(testContainer.getName(), event1),
                tuple(testContainer.getName(), event2)
            );
    }

    protected void errorWasSent(String zipFileName, ErrorCode code) {
        ArgumentCaptor<ErrorMsg> argument = ArgumentCaptor.forClass(ErrorMsg.class);
        verify(serviceBusHelper).sendMessage(argument.capture());

        ErrorMsg sentMsg = argument.getValue();

        assertThat(sentMsg.zipFileName).isEqualTo(zipFileName);
        assertThat(sentMsg.jurisdiction).isEqualTo(CONTAINER_NAME);
        assertThat(sentMsg.errorCode).isEqualTo(code);
        assertThat(sentMsg.poBox).isEqualTo(PO_BOX);
        assertThat(sentMsg.container).isEqualTo(CONTAINER_NAME);
        assertThat(sentMsg.documentControlNumber).isNull();
        assertThat(sentMsg.service).isEqualTo("bulk_scan_processor");
        assertThat(sentMsg.errorCode).isEqualTo(code);
        assertThat(sentMsg.errorDescription).isNotEmpty();
    }

    protected void envelopeWasNotCreated() {
        List<Envelope> envelopesInDb = envelopeRepository.findAll();
        assertThat(envelopesInDb).isEmpty();
    }

    protected void fileWasDeleted(String fileName) throws Exception {
        CloudBlockBlob blob = testContainer.getBlockBlobReference(fileName);
        await("file should be deleted").timeout(2, SECONDS).until(blob::exists, is(false));
    }
}
