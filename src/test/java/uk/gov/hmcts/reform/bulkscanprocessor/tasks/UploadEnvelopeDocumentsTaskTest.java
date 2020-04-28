package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

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
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.CREATED;

@ExtendWith(MockitoExtension.class)
class UploadEnvelopeDocumentsTaskTest {

    private static final String CONTAINER_1 = "container-1";
    private static final String CONTAINER_2 = "container-2";
    private static final String ZIP_FILE_NAME = "zip-file-name";

    @Mock private EnvelopeRepository envelopeRepository;
    @Mock private UploadEnvelopeDocumentsService uploadService;

    @Test
    void should_do_nothing_when_no_envelopes_to_process_are_found() {
        // given
        given(envelopeRepository.findByStatus(CREATED)).willReturn(emptyList());

        // when
        getTask(0).run();

        // then
        verifyNoInteractions(uploadService);
        verifyNoMoreInteractions(envelopeRepository);
    }

    @Test
    void should_do_nothing_when_envelope_is_not_yet_ready_to_be_processed() {
        // given
        given(envelopeRepository.findByStatus(CREATED)).willReturn(singletonList(getEnvelope()));

        // when
        getTask(1).run();

        // then
        verifyNoInteractions(uploadService);
        verifyNoMoreInteractions(envelopeRepository);
    }

    // will verify grouping by container and throwing different error
    // so both exception branches are covered in a single test
    @Test
    void should_do_nothing_when_failing_to_get_container_client() {
        // given
        List<Envelope> envelopes = Arrays.asList(
            getEnvelope(CONTAINER_1),
            getEnvelope(CONTAINER_2)
        );
        given(envelopeRepository.findByStatus(CREATED)).willReturn(envelopes);

        // when
        getTask(0).run();

        // then
        verifyNoMoreInteractions(envelopeRepository);

        // and
        verify(uploadService, times(2)).processByContainer(anyString(), anyList());
    }

    private UploadEnvelopeDocumentsTask getTask(long minimumEnvelopeAge) {
        return new UploadEnvelopeDocumentsTask(
            minimumEnvelopeAge,
            envelopeRepository,
            uploadService
        );
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

    private Envelope getEnvelope() {
        return getEnvelope(CONTAINER_1);
    }
}
