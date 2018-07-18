package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.palantir.docker.compose.DockerComposeRule;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Event;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItemRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.DocumentNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.DocumentManagementService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.output.Pdf;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.DocumentProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.io.Resources.getResource;
import static com.google.common.io.Resources.toByteArray;
import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.jsonpath.JsonPath.parse;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Event.DOC_PROCESSED;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Event.DOC_UPLOADED;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Event.DOC_UPLOAD_FAILURE;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class BlobProcessorTaskTest {

    private static final String DOCUMENT_URL1 = "http://localhost:8080/documents/1971cadc-9f79-4e1d-9033-84543bbbbc1d";
    private static final String DOCUMENT_URL2 = "http://localhost:8080/documents/0fa1ab60-f836-43aa-8c65-b07cc9bebcbe";

    @ClassRule
    public static DockerComposeRule docker = DockerComposeRule.builder()
        .file("src/integrationTest/resources/docker-compose.yml")
        .build();

    private CloudBlobClient cloudBlobClient;

    private BlobProcessorTask blobProcessorTask;

    @Autowired
    private EnvelopeRepository envelopeRepository;

    @Autowired
    private ProcessEventRepository processEventRepository;

    @Autowired
    private ScannableItemRepository scannableItemRepository;

    @Mock
    private DocumentManagementService documentManagementService;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthTokenValidator tokenValidator;

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
            processEventRepository
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
        processEventRepository.deleteAll();
    }

    @Test
    public void should_read_from_blob_storage_and_save_metadata_in_database_when_zip_contains_metadata_and_pdfs()
        throws Exception {
        //Given
        uploadZipToBlobStore("1_24-06-2018-00-00-00.zip"); //Zip file with metadata and pdfs

        byte[] test1PdfBytes = toByteArray(getResource("1111001.pdf"));
        byte[] test2PdfBytes = toByteArray(getResource("1111002.pdf"));

        Pdf pdf1 = new Pdf("1111001.pdf", test1PdfBytes);
        Pdf pdf2 = new Pdf("1111002.pdf", test2PdfBytes);

        given(documentManagementService.uploadDocuments(asList(pdf1, pdf2)))
            .willReturn(getFileUploadResponse());

        //when
        blobProcessorTask.processBlobs();

        //then
        //We expect only one envelope which was uploaded
        Envelope actualEnvelope = envelopeRepository.findAll().get(0);

        String originalMetaFile = Resources.toString(
            getResource("metadata.json"),
            Charset.defaultCharset()
        );

        assertThat(parse(actualEnvelope)).isEqualToIgnoringGivenFields(
            parse(originalMetaFile),
            "id", "zip_file_created_date", "amount", "amount_in_pence", "configuration", "json"
        );
        assertThat(actualEnvelope.getLastEvent()).isEqualTo(DOC_PROCESSED);
        assertThat(actualEnvelope.getScannableItems())
            .extracting("documentUrl")
            .hasSameElementsAs(asList(DOCUMENT_URL1, DOCUMENT_URL2));

        //This verifies pdf file objects were created from the zip file
        verify(documentManagementService).uploadDocuments(asList(pdf1, pdf2));

        // and
        List<ProcessEvent> processEvents = processEventRepository.findAll();
        assertThat(processEvents).hasSize(2);


        assertThat(processEvents)
            .extracting("container", "zipFileName", "event", "envelope.id")
            .contains(tuple(testContainer.getName(), "1_24-06-2018-00-00-00.zip", DOC_UPLOADED, actualEnvelope.getId()),
                tuple(testContainer.getName(), "1_24-06-2018-00-00-00.zip", DOC_PROCESSED, actualEnvelope.getId()));

        assertThat(processEvents).extracting("id").hasSize(2);
        assertThat(processEvents).extracting("reason").containsOnlyNulls();
    }

    @Test
    public void should_not_store_documents_in_doc_store_when_zip_does_not_contain_metadata_json() throws Exception {
        //Given
        uploadZipToBlobStore("2_24-06-2018-00-00-00.zip"); //Zip file with only pdfs and no metadata

        //when
        blobProcessorTask.processBlobs();

        //then
        List<Envelope> envelopesInDb = envelopeRepository.findAll();

        assertThat(envelopesInDb).isEmpty();

        verifyZeroInteractions(documentManagementService);
    }

    @Test
    public void should_process_other_zip_files_if_previous_zip_fails_to_process() throws Exception {
        //Given
        uploadZipToBlobStore("3_24-06-2018-00-00-00.zip"); //Zip with only pdf without metadata
        uploadZipToBlobStore("4_24-06-2018-00-00-00.zip"); //Zip with pdf and metadata

        byte[] test2PdfBytes = toByteArray(getResource("1111002.pdf"));

        Pdf pdf2 = new Pdf("1111002.pdf", test2PdfBytes);

        given(documentManagementService.uploadDocuments(asList(pdf2)))
            .willReturn(getFileUploadResponse());

        //when
        blobProcessorTask.processBlobs();

        //then
        //We expect only one envelope 4_24-06-2018-00-00-00.zip which was uploaded
        Envelope actualEnvelope = envelopeRepository.findAll().get(0);

        String originalMetaFile = Resources.toString(
            getResource("metadata_4_24-06-2018-00-00-00.json"),
            Charset.defaultCharset()
        );

        assertThat(parse(actualEnvelope)).isEqualToIgnoringGivenFields(
            parse(originalMetaFile),
            "id", "zip_file_created_date", "amount", "amount_in_pence", "configuration", "json"
        );

        //This verifies only pdf included in the zip with metadata was processed
        verify(documentManagementService).uploadDocuments(asList(pdf2));

        //Verify first pdf file was never processed
        byte[] test1PdfBytes = toByteArray(getResource("1111001.pdf"));

        verify(documentManagementService, times(0))
            .uploadDocuments(asList(new Pdf("1111001.pdf", test1PdfBytes)));
    }

    @Test
    public void should_not_save_metadata_information_in_db_when_metadata_parsing_fails() throws Exception {
        //Given
        //Invalid deliverydate and openingdate
        uploadZipToBlobStore("6_24-06-2018-00-00-00.zip"); //Zip file with pdf and invalid metadata

        //when
        blobProcessorTask.processBlobs();

        //then
        List<Envelope> envelopesInDb = envelopeRepository.findAll();

        assertThat(envelopesInDb).isEmpty();

        verifyZeroInteractions(documentManagementService);

    }

    @Test
    public void should_not_save_metadata_information_in_db_when_zip_contains_documents_not_in_pdf_format()
        throws Exception {
        //Given
        uploadZipToBlobStore("5_24-06-2018-00-00-00.zip"); // Zip file with cheque gif and metadata

        //when
        blobProcessorTask.processBlobs();

        //then
        List<Envelope> envelopesInDb = envelopeRepository.findAll();

        assertThat(envelopesInDb).isEmpty();

        verifyZeroInteractions(documentManagementService);
    }

    @Test
    public void should_delete_blob_after_doc_upload_and_mark_envelope_status_as_processed_and_create_new_event()
        throws Exception {
        // Zip with pdf and metadata
        String zipFile = "4_24-06-2018-00-00-00.zip";

        uploadZipToBlobStore(zipFile);

        byte[] testPdfBytes = toByteArray(getResource("1111002.pdf"));
        Pdf pdf = new Pdf("1111002.pdf", testPdfBytes);

        given(documentManagementService.uploadDocuments(ImmutableList.of(pdf)))
            .willReturn(getFileUploadResponse());

        given(tokenValidator.getServiceName("testServiceAuthHeader")).willReturn("test_service");

        blobProcessorTask.processBlobs();

        //Check blob is deleted
        CloudBlockBlob blob = testContainer.getBlockBlobReference(zipFile);
        await().atMost(2, SECONDS).until(blob::exists, is(false));

        //Verify envelope status is updated to DOC_PROCESSED
        mockMvc.perform(get("/envelopes")
            .header("ServiceAuthorization", "testServiceAuthHeader"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.envelopes[0].last_event", is("DOC_PROCESSED")));

        //Check events created
        List<Event> actualEvents = processEventRepository.findAll().stream()
            .map(ProcessEvent::getEvent)
            .collect(Collectors.toList());

        assertThat(actualEvents).containsOnly(DOC_UPLOADED, DOC_PROCESSED);

        verify(tokenValidator).getServiceName("testServiceAuthHeader");
    }

    @Test
    public void should_keep_zip_file_after_unsuccessful_upload_and_not_create_doc_processed_event() throws Exception {
        // Zip with pdf and metadata
        String zipFile = "4_24-06-2018-00-00-00.zip";

        uploadZipToBlobStore(zipFile);

        byte[] testPdfBytes = toByteArray(getResource("1111002.pdf"));
        Pdf pdf = new Pdf("1111002.pdf", testPdfBytes);

        given(documentManagementService.uploadDocuments(ImmutableList.of(pdf)))
            .willThrow(DocumentNotFoundException.class);

        given(tokenValidator.getServiceName("testServiceAuthHeader")).willReturn("test_service");

        blobProcessorTask.processBlobs();

        CloudBlockBlob blob = testContainer.getBlockBlobReference(zipFile);
        await().timeout(2, SECONDS).until(blob::exists, is(true));

        //Verify envelope status is updated to DOC_UPLOAD_FAILED
        mockMvc.perform(get("/envelopes")
            .header("ServiceAuthorization", "testServiceAuthHeader"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.envelopes[0].last_event", is("DOC_UPLOAD_FAILURE")));

        //Check events created
        List<Event> actualEvents = processEventRepository.findAll().stream()
            .map(ProcessEvent::getEvent)
            .collect(Collectors.toList());

        assertThat(actualEvents).containsOnly(DOC_UPLOAD_FAILURE);

        verify(tokenValidator).getServiceName("testServiceAuthHeader");
    }

    private void uploadZipToBlobStore(String fileName) throws Exception {
        byte[] zipFile = toByteArray(getResource(fileName));

        CloudBlockBlob blockBlobReference = testContainer.getBlockBlobReference(fileName);
        blockBlobReference.uploadFromByteArray(zipFile, 0, zipFile.length);
    }

    private Map<String, String> getFileUploadResponse() {
        return ImmutableMap.of(
            "1111001.pdf", DOCUMENT_URL1,
            "1111002.pdf", DOCUMENT_URL2
        );
    }
}
