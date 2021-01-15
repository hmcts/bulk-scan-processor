package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EnvelopeNotCompletedOrStaleException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EnvelopeNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EnvelopeProcessedInCcdException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.COMPLETED;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.NOTIFICATION_SENT;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.UPLOADED;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification.SUPPLEMENTARY_EVIDENCE_WITH_OCR;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.MANUAL_RETRIGGER_PROCESSING;

@ExtendWith(MockitoExtension.class)
class EnvelopeReprocessServiceTest {
    private EnvelopeReprocessService envelopeReprocessService;

    @Mock
    private EnvelopeRepository envelopeRepository;

    @Mock
    private ProcessEventRepository processEventRepository;

    @BeforeEach
    void setUp() {
        envelopeReprocessService = new EnvelopeReprocessService(
            envelopeRepository,
            processEventRepository,
            1
        );
    }

    @Test
    void should_throw_exception_if_envelope_does_not_exist() {
        // given
        var uuid = UUID.randomUUID();

        given(envelopeRepository.findById(uuid)).willReturn(Optional.empty());

        // when
        // then
        assertThatThrownBy(() ->
            envelopeReprocessService.reprocessEnvelope(uuid.toString())
        )
            .isInstanceOf(EnvelopeNotFoundException.class)
            .hasMessageContaining("Envelope with id " + uuid.toString() + " not found");
    }

    @Test
    void should_save_envelope_and_event_if_envelope_has_completed_status() {
        // given
        var uuid = UUID.randomUUID();
        var envelope = envelope(
            COMPLETED,
            null,
            null
        );
        given(envelopeRepository.findById(uuid)).willReturn(Optional.of(envelope));

        // when
        envelopeReprocessService.reprocessEnvelope(uuid.toString());

        // then
        var processEventCaptor = ArgumentCaptor.forClass(ProcessEvent.class);
        verify(processEventRepository).save(processEventCaptor.capture());
        assertThat(processEventCaptor.getValue().getContainer())
            .isEqualTo(envelope.getContainer());
        assertThat(processEventCaptor.getValue().getZipFileName())
            .isEqualTo(envelope.getZipFileName());
        assertThat(processEventCaptor.getValue().getEvent()).isEqualTo(MANUAL_RETRIGGER_PROCESSING);
        assertThat(processEventCaptor.getValue().getReason())
            .isEqualTo("Moved to UPLOADED status to reprocess the envelope");

        var envelopeCaptor = ArgumentCaptor.forClass(Envelope.class);
        verify(envelopeRepository).save(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getValue().getStatus()).isEqualTo(UPLOADED);
        assertThat(envelopeCaptor.getValue().getId()).isEqualTo(envelope.getId());
    }

    @Test
    void should_save_envelope_and_event_if_envelope_is_stale() {
        // given
        Instant twoHoursAgo = Instant.now().minus(2, HOURS);
        Instant threeHoursAgo = Instant.now().minus(3, HOURS);
        Instant fourHoursAgo = Instant.now().minus(4, HOURS);
        ProcessEvent event1 = new ProcessEvent();
        event1.setCreatedAt(twoHoursAgo);
        ProcessEvent event2 = new ProcessEvent();
        event2.setCreatedAt(threeHoursAgo);
        ProcessEvent event3 = new ProcessEvent();
        event3.setCreatedAt(fourHoursAgo);

        var envelope = envelope(
            NOTIFICATION_SENT,
            null,
            null
        );
        given(processEventRepository.findByZipFileName(envelope.getZipFileName()))
            .willReturn(asList(event1, event2, event3));

        var uuid = UUID.randomUUID();
        given(envelopeRepository.findById(uuid)).willReturn(Optional.of(envelope));

        // when
        envelopeReprocessService.reprocessEnvelope(uuid.toString());

        // then
        var processEventCaptor = ArgumentCaptor.forClass(ProcessEvent.class);
        verify(processEventRepository).save(processEventCaptor.capture());
        assertThat(processEventCaptor.getValue().getContainer())
            .isEqualTo(envelope.getContainer());
        assertThat(processEventCaptor.getValue().getZipFileName())
            .isEqualTo(envelope.getZipFileName());
        assertThat(processEventCaptor.getValue().getEvent()).isEqualTo(MANUAL_RETRIGGER_PROCESSING);
        assertThat(processEventCaptor.getValue().getReason())
            .isEqualTo("Moved to UPLOADED status to reprocess the envelope");

        var envelopeCaptor = ArgumentCaptor.forClass(Envelope.class);
        verify(envelopeRepository).save(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getValue().getStatus()).isEqualTo(UPLOADED);
        assertThat(envelopeCaptor.getValue().getId()).isEqualTo(envelope.getId());
    }

    @Test
    void should_throw_excption_if_envelope_is_not_stale() {
        // given
        Instant halfHourAgo = Instant.now().minus(30, MINUTES);
        Instant threeHoursAgo = Instant.now().minus(3, HOURS);
        Instant fourHoursAgo = Instant.now().minus(4, HOURS);
        ProcessEvent event1 = new ProcessEvent();
        event1.setCreatedAt(halfHourAgo);
        ProcessEvent event2 = new ProcessEvent();
        event2.setCreatedAt(threeHoursAgo);
        ProcessEvent event3 = new ProcessEvent();
        event3.setCreatedAt(fourHoursAgo);

        var envelope = envelope(
            NOTIFICATION_SENT,
            null,
            null
        );
        given(processEventRepository.findByZipFileName(envelope.getZipFileName()))
            .willReturn(asList(event1, event2, event3));

        var uuid = UUID.randomUUID();
        given(envelopeRepository.findById(uuid)).willReturn(Optional.of(envelope));

        // when
        // then
        assertThatThrownBy(() ->
            envelopeReprocessService.reprocessEnvelope(uuid.toString())
        )
            .isInstanceOf(EnvelopeNotCompletedOrStaleException.class)
            .hasMessageMatching("^(Envelope with id )[\\S]+( is not completed or stale)$");
    }

    @Test
    void should_throw_exception_if_envelope_has_uploaded_status() {
        // given
        var uuid = UUID.randomUUID();
        var envelope = envelope(
            UPLOADED,
            null,
            null
        );
        given(envelopeRepository.findById(uuid)).willReturn(Optional.of(envelope));

        // when
        // then
        assertThatThrownBy(() ->
            envelopeReprocessService.reprocessEnvelope(uuid.toString())
        )
            .isInstanceOf(EnvelopeNotCompletedOrStaleException.class)
            .hasMessageMatching("^(Envelope with id )[\\S]+( is not completed or stale)$");
    }

    @Test
    void should_throw_exception_if_envelope_has_ccdid() {
        // given
        var uuid = UUID.randomUUID();
        var envelope = envelope(
            NOTIFICATION_SENT,
            "111222333",
            "create"
        );
        given(envelopeRepository.findById(uuid)).willReturn(Optional.of(envelope));

        // when
        // then
        assertThatThrownBy(() ->
            envelopeReprocessService.reprocessEnvelope(uuid.toString())
        )
            .isInstanceOf(EnvelopeProcessedInCcdException.class)
            .hasMessageMatching("^(Envelope with id )[\\S]+( has already been processed in CCD)$");
    }

    private Envelope envelope(
        Status status,
        String ccdId,
        String ccdAction
    ) {
        Envelope envelope = new Envelope(
            "poBox",
            "jurisdiction1",
            Instant.now(),
            Instant.now(),
            Instant.now(),
            "fileName.zip",
            "1234432112344321",
            null,
            SUPPLEMENTARY_EVIDENCE_WITH_OCR,
            emptyList(),
            emptyList(),
            emptyList(),
            "container",
            null
        );

        envelope.setStatus(status);

        envelope.setCcdId(ccdId);
        envelope.setEnvelopeCcdAction(ccdAction);

        return envelope;
    }
}
