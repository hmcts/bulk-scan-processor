package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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
import uk.gov.hmcts.reform.bulkscanprocessor.services.EnvelopeHandler;
import uk.gov.hmcts.reform.bulkscanprocessor.services.ErrorNotificationSender;
import uk.gov.hmcts.reform.bulkscanprocessor.services.FileContentProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.services.FileRejector;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.DocumentManagementService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.ServiceBusSendHelper;
import uk.gov.hmcts.reform.bulkscanprocessor.services.storage.LeaseAcquirer;
import uk.gov.hmcts.reform.bulkscanprocessor.services.storage.OcrValidationRetryManager;
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

import static com.jayway.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;

public abstract class ProcessorTestSuite {

    protected static final String SAMPLE_ZIP_FILE_NAME = "1_24-06-2018-00-00-00.zip";

    protected static final String DOCUMENT_URL1 =
        "http://localhost:8080/documents/1971cadc-9f79-4e1d-9033-84543bbbbc1d";
    protected static final String DOCUMENT_URL2 =
        "http://localhost:8080/documents/0fa1ab60-f836-43aa-8c65-b07cc9bebcbe";
    protected static final String DOCUMENT_UUID2 = "0fa1ab60-f836-43aa-8c65-b07cc9bebcbe";

    protected static final String CONTAINER_NAME = "bulkscan";
    protected static final String PO_BOX = "BULKSCANPO";
    protected static final String REJECTED_CONTAINER_NAME = "bulkscan-rejected";

    protected BlobProcessorTask processor;

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

    @Autowired
    protected OcrValidationRetryManager ocrValidationRetryManager;

    protected ErrorNotificationSender errorNotificationSender;

    protected EnvelopeValidator envelopeValidator;

    protected FileRejector fileRejector;

    protected FileContentProcessor fileContentProcessor;

    protected EnvelopeHandler envelopeHandler;

    protected DocumentProcessor documentProcessor;

    protected EnvelopeProcessor envelopeProcessor;

    protected BlobManager blobManager;

    @Autowired
    private ScannableItemRepository scannableItemRepository;

    @Autowired
    private BlobManagementProperties blobManagementProperties;

    @Autowired
    private LeaseAcquirer leaseAcquirer;

    @Mock
    protected DocumentManagementService documentManagementService;

    @Mock
    protected OcrValidator ocrValidator;

    @Mock
    protected ServiceBusSendHelper serviceBusHelper;

    @Value("${process-payments.enabled}")
    protected boolean paymentsEnabled;

    protected BlobContainerClient testContainer;
    protected BlobContainerClient rejectedContainer;

    private static DockerComposeContainer dockerComposeContainer;
    private static String dockerHost;

    @BeforeEach
    public void setUp() {

        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
            .connectionString(String.format(TestStorageHelper.STORAGE_CONN_STRING, dockerHost, 10000))
            .buildClient();

        blobManager = new BlobManager(blobServiceClient, blobManagementProperties);

        documentProcessor = new DocumentProcessor(
            documentManagementService,
            scannableItemRepository
        );

        envelopeProcessor = new EnvelopeProcessor(
            schemaValidator,
            envelopeRepository,
            processEventRepository
        );

        errorNotificationSender = new ErrorNotificationSender(
            serviceBusHelper,
            containerMappings
        );

        envelopeValidator = new EnvelopeValidator();

        fileRejector = new FileRejector(
            blobManager,
            errorNotificationSender
        );

        envelopeHandler = new EnvelopeHandler(
            envelopeValidator,
            containerMappings,
            envelopeProcessor,
            ocrValidator,
            paymentsEnabled
        );

        fileContentProcessor = new FileContentProcessor(
            zipFileProcessor,
            envelopeProcessor,
            envelopeHandler,
            fileRejector
        );

        testContainer = blobServiceClient.getBlobContainerClient(CONTAINER_NAME);
        if (!testContainer.exists()) {
            testContainer.create();
        }

        rejectedContainer = blobServiceClient.getBlobContainerClient(REJECTED_CONTAINER_NAME);
        if (!rejectedContainer.exists()) {
            rejectedContainer.create();
        }

        processor = new BlobProcessorTask(
            blobManager,
            envelopeProcessor,
            fileContentProcessor,
            leaseAcquirer,
            ocrValidationRetryManager
        );
    }

    @AfterEach
    public void cleanUp() {
        if (testContainer.exists()) {
            testContainer.delete();
        }
        if (rejectedContainer.exists()) {
            rejectedContainer.delete();
        }
        envelopeRepository.deleteAll();
        processEventRepository.deleteAll();
    }

    @BeforeEach
    public void prepare() {
        envelopeRepository.deleteAll();
        processEventRepository.deleteAll();
    }

    @BeforeAll
    public static void initialize() {
        File dockerComposeFile = new File("src/integrationTest/resources/docker-compose.yml");

        dockerComposeContainer = new DockerComposeContainer(dockerComposeFile)
            .withExposedService("azure-storage", 10000);

        dockerComposeContainer.start();
        dockerHost = dockerComposeContainer.getServiceHost("azure-storage", 10000);
    }

    @AfterAll
    public static void tearDownContainer() {
        dockerComposeContainer.stop();
    }

    public void uploadToBlobStorage(String fileName, byte[] fileContent) {
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
                tuple(testContainer.getBlobContainerName(), event1),
                tuple(testContainer.getBlobContainerName(), event2)
            );
    }

    protected void eventsWereCreated(Event[] events) {
        assertThat(processEventRepository.findAll()).hasSizeGreaterThan(0)
            .extracting(e -> tuple(e.getContainer(), e.getEvent()))
            .containsExactlyInAnyOrder(
                tuple(testContainer.getBlobContainerName(), events)
            );
    }

    protected void errorWasSent(String zipFileName, ErrorCode code) {
        errorWasSent(zipFileName, code, null);
    }

    protected void errorWasSent(String zipFileName, ErrorCode code, String errorMessage) {
        ArgumentCaptor<ErrorMsg> argument = ArgumentCaptor.forClass(ErrorMsg.class);
        verify(serviceBusHelper).sendMessage(argument.capture());

        ErrorMsg sentMsg = argument.getValue();

        assertThat(sentMsg.id).isEqualTo(CONTAINER_NAME + "_" + zipFileName);
        assertThat(sentMsg.zipFileName).isEqualTo(zipFileName);
        assertThat(sentMsg.jurisdiction).isEqualTo(CONTAINER_NAME);
        assertThat(sentMsg.errorCode).isEqualTo(code);
        assertThat(sentMsg.poBox).isEqualTo(PO_BOX);
        assertThat(sentMsg.container).isEqualTo(CONTAINER_NAME);
        assertThat(sentMsg.documentControlNumber).isNull();
        assertThat(sentMsg.service).isEqualTo("bulk_scan_processor");
        assertThat(sentMsg.errorCode).isEqualTo(code);
        assertThat(sentMsg.errorDescription).isNotEmpty();
        if (errorMessage != null) {
            assertThat(sentMsg.errorDescription).isEqualTo(errorMessage);
        }
    }

    protected void envelopeWasNotCreated() {
        List<Envelope> envelopesInDb = envelopeRepository.findAll();
        assertThat(envelopesInDb).isEmpty();
    }

    protected void fileWasDeleted(String fileName) {
        BlobClient blobClient = testContainer.getBlobClient(fileName);
        await("file should be deleted").timeout(2, SECONDS).until(blobClient::exists, is(false));
    }
}
