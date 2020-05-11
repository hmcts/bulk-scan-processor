package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.bulkscanprocessor.config.IntegrationTest;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputEnvelope;
import uk.gov.hmcts.reform.bulkscanprocessor.model.mapper.EnvelopeMapper;
import uk.gov.hmcts.reform.bulkscanprocessor.services.UploadEnvelopeDocumentsService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.DocumentManagementService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.output.Pdf;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.util.TestStorageHelper;

import java.util.Optional;
import java.util.UUID;

import static com.google.common.io.Resources.getResource;
import static com.google.common.io.Resources.toByteArray;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.UPLOADED;

@IntegrationTest
@RunWith(SpringRunner.class)
public class UploadEnvelopeDocumentsTaskTest {

    private static final TestStorageHelper STORAGE_HELPER = TestStorageHelper.getInstance();

    @Autowired private EnvelopeProcessor envelopeProcessor;
    @Autowired private EnvelopeRepository envelopeRepository;
    @Autowired private ProcessEventRepository eventRepository;
    @Autowired private UploadEnvelopeDocumentsService uploadService;

    @MockBean private AuthTokenGenerator tokenGenerator;
    @MockBean private DocumentManagementService documentManagementService;

    @BeforeClass
    public static void initializeStorage() {
        TestStorageHelper.initialize();
    }

    @AfterClass
    public static void tearDownContainer() {
        TestStorageHelper.stopDocker();
    }

    @Before
    public void prepare() {
        STORAGE_HELPER.createBulkscanContainer();
        envelopeRepository.deleteAll();
        eventRepository.deleteAll();
    }

    @After
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
        Pdf pdf = new Pdf("1111002.pdf", toByteArray(getResource("zipcontents/ok/1111002.pdf")));

        given(tokenGenerator.generate()).willReturn("token");
        given(documentManagementService.uploadDocuments(ImmutableList.of(pdf)))
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
        new UploadEnvelopeDocumentsTask(envelopeRepository, uploadService).run();

        // then
        assertThat(envelopeRepository.findById(envelopeId))
            .isNotEmpty()
            .get()
            .extracting(Envelope::getStatus)
            .isEqualTo(UPLOADED);
    }
}
