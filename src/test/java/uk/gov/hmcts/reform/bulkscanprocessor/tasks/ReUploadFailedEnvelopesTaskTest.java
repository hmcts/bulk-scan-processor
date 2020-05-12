package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification;
import uk.gov.hmcts.reform.bulkscanprocessor.services.UploadEnvelopeDocumentsService;

import java.util.Arrays;
import java.util.List;

import static java.time.Instant.now;
import static java.util.Collections.emptyList;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class ReUploadFailedEnvelopesTaskTest {

    private static final int MAX_RE_UPLOAD_TRIES = 5;
    private static final String CONTAINER_1 = "container-1";
    private static final String CONTAINER_2 = "container-2";
    private static final String ZIP_FILE_NAME = "zip-file-name";

    @Mock private EnvelopeRepository envelopeRepository;
    @Mock private UploadEnvelopeDocumentsService uploadService;

    private ReuploadFailedEnvelopeTask task;

    @BeforeEach
    private void setUp() {
        task = new ReuploadFailedEnvelopeTask(envelopeRepository, uploadService, MAX_RE_UPLOAD_TRIES);
    }

    @Test
    void should_do_nothing_when_no_envelopes_to_process_are_found() {
        // given
        given(envelopeRepository.findEnvelopesToResend(MAX_RE_UPLOAD_TRIES)).willReturn(emptyList());

        // when
        task.processUploadFailures();

        // then
        verifyNoInteractions(uploadService);
        verifyNoMoreInteractions(envelopeRepository);
    }

    // will verify grouping by container and throwing different error
    // so both exception branches are covered in a single test
    @Test
    void should_call_service_twice_for_individual_containers() {
        // given
        List<Envelope> envelopes = Arrays.asList(
            getEnvelope(CONTAINER_1),
            getEnvelope(CONTAINER_2)
        );
        given(envelopeRepository.findEnvelopesToResend(MAX_RE_UPLOAD_TRIES)).willReturn(envelopes);

        // when
        task.processUploadFailures();

        // then
        verifyNoMoreInteractions(envelopeRepository);

        // and
        verify(uploadService, times(2)).processByContainer(anyString(), anyList());
    }

    private Envelope getEnvelope(String containerName) {
        return new Envelope(
            "po-box",
            "jurisdiction",
            now(), // delivery date
            now(), // opening date
            now(), // zip file created at (from blob storage)
            ZIP_FILE_NAME,
            "case-number",
            "previous-service-case-reference",
            Classification.EXCEPTION,
            emptyList(),
            emptyList(),
            emptyList(),
            containerName
        );
    }
}
