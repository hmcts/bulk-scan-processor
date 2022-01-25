package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.bulkscanprocessor.config.BlobManagementProperties;
import uk.gov.hmcts.reform.bulkscanprocessor.config.IntegrationTest;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputEnvelope;
import uk.gov.hmcts.reform.bulkscanprocessor.model.mapper.EnvelopeMapper;
import uk.gov.hmcts.reform.bulkscanprocessor.services.UploadEnvelopeDocumentsService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.DocumentManagementService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.storage.LeaseAcquirer;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.BlobManager;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.DocumentProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.ZipFileProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.util.TestStorageHelper;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.google.common.io.Resources.getResource;
import static com.google.common.io.Resources.toByteArray;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.UPLOADED;

@IntegrationTest
@SuppressWarnings("unchecked")
public class UploadEnvelopeDocumentsTaskTest {

    private static final TestStorageHelper STORAGE_HELPER = TestStorageHelper.getInstance();

    @Autowired private EnvelopeProcessor envelopeProcessor;
    @Autowired private EnvelopeRepository envelopeRepository;
    @Autowired private ProcessEventRepository eventRepository;
    @Autowired private ZipFileProcessor zipFileProcessor;
    @Autowired private DocumentProcessor documentProcessor;
    @Autowired private BlobManagementProperties blobManagementProperties;
    @Autowired private LeaseAcquirer leaseAcquirer;

    @MockBean private AuthTokenGenerator tokenGenerator;
    @MockBean private DocumentManagementService documentManagementService;

    @BeforeAll
    public static void initializeStorage() {
        TestStorageHelper.initialize();
    }

    @AfterAll
    public static void tearDownContainer() {
        TestStorageHelper.stopDocker();
    }

    @BeforeEach
    public void prepare() {
        STORAGE_HELPER.createBulkscanContainer();
        envelopeRepository.deleteAll();
        eventRepository.deleteAll();
    }

    @AfterEach
    public void cleanUp() {
        STORAGE_HELPER.deleteBulkscanContainer();
        envelopeRepository.deleteAll();
        eventRepository.deleteAll();
    }

    @Test
    public void should_mark_envelope_as_uploaded() throws Exception {
        // given
        STORAGE_HELPER.upload();

        // and
        given(tokenGenerator.generate()).willReturn("token");
        given(documentManagementService.uploadDocuments(any(), eq("BULKSCAN"), eq("bulkscan")))
            .willReturn(ImmutableMap.of(
                "1111002.pdf", "http://localhost:8080/documents/" + UUID.randomUUID().toString()
            ));

        // and
        InputEnvelope inputEnvelope = envelopeProcessor.parseEnvelope(
            toByteArray(getResource("zipcontents/ok/metadata.json")),
            TestStorageHelper.ZIP_FILE_NAME
        );
        Envelope envelope = EnvelopeMapper.toDbEnvelope(
            inputEnvelope,
            TestStorageHelper.CONTAINER_NAME,
            Optional.empty()
        );
        UUID envelopeId = envelopeRepository.saveAndFlush(envelope).getId();

        // when
        BlobManager blobManager = new BlobManager(STORAGE_HELPER.blobServiceClient, blobManagementProperties);
        UploadEnvelopeDocumentsService uploadService = new UploadEnvelopeDocumentsService(
            blobManager,
            zipFileProcessor,
            documentProcessor,
            envelopeProcessor,
            leaseAcquirer
        );
        new UploadEnvelopeDocumentsTask(envelopeRepository, uploadService, 1).run();

        // then
        assertThat(envelopeRepository.findById(envelopeId))
            .isNotEmpty()
            .get()
            .extracting(Envelope::getStatus)
            .isEqualTo(UPLOADED);
        ArgumentCaptor<List<File>> pdfListCaptor = ArgumentCaptor.forClass(List.class);
        verify(documentManagementService, times(1))
            .uploadDocuments(pdfListCaptor.capture(), eq("BULKSCAN"), eq("bulkscan"));
        assertThat(pdfListCaptor.getValue().get(0).getName()).isEqualTo("1111002.pdf");
    }

}
