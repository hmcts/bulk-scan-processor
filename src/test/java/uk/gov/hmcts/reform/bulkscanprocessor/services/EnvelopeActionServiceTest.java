package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EnvelopeClassificationException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EnvelopeNotCompletedOrStaleException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EnvelopeNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EnvelopeNotInInconsistentStateException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EnvelopeProcessedInCcdException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;

import java.time.Instant;
import java.util.NoSuchElementException;
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
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.ABORTED;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.COMPLETED;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.NOTIFICATION_SENT;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.UPLOADED;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification.SUPPLEMENTARY_EVIDENCE;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification.SUPPLEMENTARY_EVIDENCE_WITH_OCR;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.DOC_PROCESSED_NOTIFICATION_SENT;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.DOC_UPLOADED;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.MANUAL_RETRIGGER_PROCESSING;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.MANUAL_STATUS_CHANGE;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.ZIPFILE_PROCESSING_STARTED;

@ExtendWith(MockitoExtension.class)
class EnvelopeActionServiceTest {
    private EnvelopeActionService envelopeActionService;

    @Mock
    private EnvelopeRepository envelopeRepository;

    @Mock
    private ProcessEventRepository processEventRepository;

    @BeforeEach
    void setUp() {
        envelopeActionService = new EnvelopeActionService(
            envelopeRepository,
            processEventRepository,
            1
        );
    }

    @Test
    void reprocessEnvelope_should_throw_exception_if_envelope_does_not_exist() {
        // given
        var uuid = UUID.randomUUID();

        given(envelopeRepository.findById(uuid)).willReturn(Optional.empty());

        // when
        // then
        assertThatThrownBy(() -> envelopeActionService.reprocessEnvelope(uuid))
            .isInstanceOf(EnvelopeNotFoundException.class)
            .hasMessageContaining("Envelope with id " + uuid + " not found");
    }

    @Test
    void reprocessEnvelope_should_save_envelope_and_event_if_envelope_has_completed_status() {
        // given
        var uuid = UUID.randomUUID();
        var envelope = envelope(
            COMPLETED,
            null,
            null
        );
        given(envelopeRepository.findById(uuid)).willReturn(Optional.of(envelope));

        // when
        envelopeActionService.reprocessEnvelope(uuid);

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
    void reprocessEnvelope_should_save_envelope_and_event_if_envelope_is_stale() {
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
        given(processEventRepository.findByZipFileNameOrderByCreatedAtDesc(envelope.getZipFileName()))
            .willReturn(asList(event1, event2, event3));

        var uuid = UUID.randomUUID();
        given(envelopeRepository.findById(uuid)).willReturn(Optional.of(envelope));

        // when
        envelopeActionService.reprocessEnvelope(uuid);

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
    void reprocessEnvelope_should_throw_exception_if_envelope_is_not_stale() {
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
        given(processEventRepository.findByZipFileNameOrderByCreatedAtDesc(envelope.getZipFileName()))
            .willReturn(asList(event1, event2, event3));

        var uuid = UUID.randomUUID();
        given(envelopeRepository.findById(uuid)).willReturn(Optional.of(envelope));

        // when
        // then
        assertThatThrownBy(() -> envelopeActionService.reprocessEnvelope(uuid))
            .isInstanceOf(EnvelopeNotCompletedOrStaleException.class)
            .hasMessageMatching("^(Envelope with id )[\\S]+( is not completed, aborted or stale)$");
    }

    @Test
    void reprocessEnvelope_should_throw_exception_if_no_events_for_envelope() {
        // given
        var envelope = envelope(
            NOTIFICATION_SENT,
            null,
            null
        );
        given(processEventRepository.findByZipFileNameOrderByCreatedAtDesc(envelope.getZipFileName()))
            .willReturn(emptyList());

        var uuid = UUID.randomUUID();
        given(envelopeRepository.findById(uuid)).willReturn(Optional.of(envelope));

        // when
        // then
        assertThatThrownBy(() -> envelopeActionService.reprocessEnvelope(uuid))
            .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void reprocessEnvelope_should_throw_exception_if_envelope_has_uploaded_status() {
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
        assertThatThrownBy(() -> envelopeActionService.reprocessEnvelope(uuid))
            .isInstanceOf(EnvelopeNotCompletedOrStaleException.class)
            .hasMessageMatching("^(Envelope with id )[\\S]+( is not completed, aborted or stale)$");
    }

    @Test
    void reprocessEnvelope_should_throw_exception_if_envelope_has_ccdid() {
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
        assertThatThrownBy(() -> envelopeActionService.reprocessEnvelope(uuid))
            .isInstanceOf(EnvelopeProcessedInCcdException.class)
            .hasMessageMatching("^(Envelope with id )[\\S]+( has already been processed in CCD)$");
    }

    @Test
    void moveEnvelopeToCompleted_should_throw_exception_if_envelope_does_not_exist() {
        // given
        var uuid = UUID.randomUUID();

        given(envelopeRepository.findById(uuid)).willReturn(Optional.empty());

        // when
        // then
        assertThatThrownBy(() ->
                               envelopeActionService.moveEnvelopeToCompleted(uuid)
        )
            .isInstanceOf(EnvelopeNotFoundException.class)
            .hasMessageContaining("Envelope with id " + uuid + " not found");
    }

    @Test
    void moveEnvelopeToCompleted_should_save_envelope_and_event_if_envelope_has_completed_status() {
        // given
        var uuid = UUID.randomUUID();
        var envelope = envelope(
            NOTIFICATION_SENT,
            "ccdId",
            null
        );
        given(envelopeRepository.findById(uuid)).willReturn(Optional.of(envelope));
        given(processEventRepository.findByZipFileNameOrderByCreatedAtDesc(envelope.getZipFileName()))
            .willReturn(asList(
                new ProcessEvent(envelope.getContainer(), envelope.getZipFileName(), ZIPFILE_PROCESSING_STARTED),
                new ProcessEvent(envelope.getContainer(), envelope.getZipFileName(), DOC_UPLOADED),
                new ProcessEvent(envelope.getContainer(), envelope.getZipFileName(), Event.COMPLETED),
                new ProcessEvent(envelope.getContainer(), envelope.getZipFileName(), DOC_PROCESSED_NOTIFICATION_SENT)
            ));

        // when
        envelopeActionService.moveEnvelopeToCompleted(uuid);

        // then
        var processEventCaptor = ArgumentCaptor.forClass(ProcessEvent.class);
        verify(processEventRepository).save(processEventCaptor.capture());
        assertThat(processEventCaptor.getValue().getContainer())
            .isEqualTo(envelope.getContainer());
        assertThat(processEventCaptor.getValue().getZipFileName())
            .isEqualTo(envelope.getZipFileName());
        assertThat(processEventCaptor.getValue().getEvent()).isEqualTo(MANUAL_STATUS_CHANGE);
        assertThat(processEventCaptor.getValue().getReason())
            .isEqualTo("Moved to COMPLETED status to fix inconsistent state caused by race conditions");

        var envelopeCaptor = ArgumentCaptor.forClass(Envelope.class);
        verify(envelopeRepository).save(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getValue().getStatus()).isEqualTo(COMPLETED);
        assertThat(envelopeCaptor.getValue().getId()).isEqualTo(envelope.getId());
    }

    @Test
    void moveEnvelopeToCompleted_should_throw_exception_if_status_is_not_notification_sent() {
        // given
        var uuid = UUID.randomUUID();
        var envelope = envelope(
            COMPLETED,
            null,
            null
        );
        given(envelopeRepository.findById(uuid)).willReturn(Optional.of(envelope));

        // when
        // then
        assertThatThrownBy(() ->
                               envelopeActionService.moveEnvelopeToCompleted(uuid)
        )
            .isInstanceOf(EnvelopeNotInInconsistentStateException.class)
            .hasMessageMatching("^(Envelope with id )[\\S]+( is not in inconsistent state)$");
    }

    @Test
    void moveEnvelopeToCompleted_should_throw_exception_if_no_completed_event() {
        // given
        var uuid = UUID.randomUUID();
        var envelope = envelope(
            NOTIFICATION_SENT,
            null,
            null
        );
        given(envelopeRepository.findById(uuid)).willReturn(Optional.of(envelope));
        given(processEventRepository.findByZipFileNameOrderByCreatedAtDesc(envelope.getZipFileName()))
            .willReturn(asList(
                new ProcessEvent(envelope.getContainer(), envelope.getZipFileName(), ZIPFILE_PROCESSING_STARTED),
                new ProcessEvent(envelope.getContainer(), envelope.getZipFileName(), DOC_UPLOADED),
                new ProcessEvent(envelope.getContainer(), envelope.getZipFileName(), DOC_PROCESSED_NOTIFICATION_SENT)
            ));

        // when
        // then
        assertThatThrownBy(() ->
                               envelopeActionService.moveEnvelopeToCompleted(uuid)
        )
            .isInstanceOf(EnvelopeNotInInconsistentStateException.class)
            .hasMessageMatching("^(Envelope with id )[\\S]+( does not have COMPLETED event)$");
    }

    @Test
    void moveEnvelopeToAborted_should_throw_exception_if_envelope_does_not_exist() {
        // given
        var uuid = UUID.randomUUID();

        given(envelopeRepository.findById(uuid)).willReturn(Optional.empty());

        // when
        // then
        assertThatThrownBy(() ->
                               envelopeActionService.moveEnvelopeToAborted(uuid)
        )
            .isInstanceOf(EnvelopeNotFoundException.class)
            .hasMessageContaining("Envelope with id " + uuid + " not found");
    }

    @Test
    void moveEnvelopeToAborted_should_save_envelope_and_event_if_envelope_is_stale() {
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

        var envelope = envelope(NOTIFICATION_SENT, null, null);
        given(processEventRepository.findByZipFileNameOrderByCreatedAtDesc(envelope.getZipFileName()))
                .willReturn(asList(event1, event2, event3));

        var uuid = UUID.randomUUID();
        given(envelopeRepository.findById(uuid)).willReturn(Optional.of(envelope));

        // when
        envelopeActionService.moveEnvelopeToAborted(uuid);

        // then
        var processEventCaptor = ArgumentCaptor.forClass(ProcessEvent.class);
        verify(processEventRepository).save(processEventCaptor.capture());
        assertThat(processEventCaptor.getValue().getContainer())
                .isEqualTo(envelope.getContainer());
        assertThat(processEventCaptor.getValue().getZipFileName())
                .isEqualTo(envelope.getZipFileName());
        assertThat(processEventCaptor.getValue().getEvent()).isEqualTo(MANUAL_STATUS_CHANGE);
        assertThat(processEventCaptor.getValue().getReason())
                .isEqualTo("Moved to ABORTED status to fix inconsistent state unresolved by the service");

        var envelopeCaptor = ArgumentCaptor.forClass(Envelope.class);
        verify(envelopeRepository).save(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getValue().getStatus()).isEqualTo(ABORTED);
        assertThat(envelopeCaptor.getValue().getId()).isEqualTo(envelope.getId());
    }

    @Test
    void moveEnvelopeToAborted_should_throw_exception_if_envelope_is_not_stale() {
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

        var envelope = envelope(NOTIFICATION_SENT, null, null);
        given(processEventRepository.findByZipFileNameOrderByCreatedAtDesc(envelope.getZipFileName()))
                .willReturn(asList(event1, event2, event3));

        var uuid = UUID.randomUUID();
        given(envelopeRepository.findById(uuid)).willReturn(Optional.of(envelope));

        // when
        // then
        assertThatThrownBy(() -> envelopeActionService.moveEnvelopeToAborted(uuid))
            .isInstanceOf(EnvelopeNotCompletedOrStaleException.class)
            .hasMessageMatching("^(Envelope with id )[\\S]+( is not completed or stale)$");
    }

    @Test
    void moveEnvelopeToAborted_should_throw_exception_if_no_events_for_envelope() {
        // given
        var envelope = envelope(NOTIFICATION_SENT, null, null);
        given(processEventRepository.findByZipFileNameOrderByCreatedAtDesc(envelope.getZipFileName()))
                .willReturn(emptyList());

        var uuid = UUID.randomUUID();
        given(envelopeRepository.findById(uuid)).willReturn(Optional.of(envelope));

        // when
        // then
        assertThatThrownBy(() -> envelopeActionService.moveEnvelopeToAborted(uuid))
            .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void moveEnvelopeToAborted_should_throw_exception_if_envelope_has_uploaded_status() {
        // given
        var uuid = UUID.randomUUID();
        var envelope = envelope(UPLOADED, null, null);
        given(envelopeRepository.findById(uuid)).willReturn(Optional.of(envelope));

        // when
        // then
        assertThatThrownBy(() -> envelopeActionService.moveEnvelopeToAborted(uuid))
            .isInstanceOf(EnvelopeNotCompletedOrStaleException.class)
            .hasMessageMatching("^(Envelope with id )[\\S]+( is not completed or stale)$");
    }

    @Test
    void moveEnvelopeToAborted_should_throw_exception_if_envelope_has_ccdid() {
        // given
        var uuid = UUID.randomUUID();
        var envelope = envelope(NOTIFICATION_SENT, "111222333", "create");
        given(envelopeRepository.findById(uuid)).willReturn(Optional.of(envelope));

        // when
        // then
        assertThatThrownBy(() -> envelopeActionService.moveEnvelopeToAborted(uuid))
            .isInstanceOf(EnvelopeProcessedInCcdException.class)
            .hasMessageMatching("^(Envelope with id )[\\S]+( has already been processed in CCD)$");
    }

    @Test
    void updateClassificationAndReprocessEnvelope_should_throw_exception_if_envelope_does_not_exist() {
        // given
        var uuid = UUID.randomUUID();

        given(envelopeRepository.findById(uuid)).willReturn(Optional.empty());

        // when
        // then
        assertThatThrownBy(() -> envelopeActionService.updateClassificationAndReprocessEnvelope(uuid))
            .isInstanceOf(EnvelopeNotFoundException.class)
            .hasMessageContaining("Envelope with id " + uuid + " not found");
    }

    @Test
    void updateClassificationAndReprocessEnvelope_should_save_envelope_and_event_if_envelope_is_stale() {
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

        var envelope = envelope(SUPPLEMENTARY_EVIDENCE, NOTIFICATION_SENT, null, null);
        given(processEventRepository.findByZipFileNameOrderByCreatedAtDesc(envelope.getZipFileName()))
            .willReturn(asList(event1, event2, event3));

        var uuid = UUID.randomUUID();
        given(envelopeRepository.findById(uuid)).willReturn(Optional.of(envelope));

        // when
        envelopeActionService.updateClassificationAndReprocessEnvelope(uuid);

        // then
        var processEventCaptor = ArgumentCaptor.forClass(ProcessEvent.class);
        verify(processEventRepository).save(processEventCaptor.capture());
        assertThat(processEventCaptor.getValue().getContainer())
            .isEqualTo(envelope.getContainer());
        assertThat(processEventCaptor.getValue().getZipFileName())
            .isEqualTo(envelope.getZipFileName());
        assertThat(processEventCaptor.getValue().getEvent()).isEqualTo(MANUAL_RETRIGGER_PROCESSING);
        assertThat(processEventCaptor.getValue().getReason())
            .isEqualTo(
                "Updated envelope classification to EXCEPTION and status to UPLOADED to reprocess the envelope");

        verify(envelopeRepository).updateEnvelopeClassificationAndStatus(uuid, envelope.getContainer());
    }

    @Test
    void updateClassificationAndReprocessEnvelope_should_throw_exception_if_envelope_is_not_stale() {
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

        var envelope = envelope(SUPPLEMENTARY_EVIDENCE, NOTIFICATION_SENT, null, null);
        given(processEventRepository.findByZipFileNameOrderByCreatedAtDesc(envelope.getZipFileName()))
            .willReturn(asList(event1, event2, event3));

        var uuid = UUID.randomUUID();
        given(envelopeRepository.findById(uuid)).willReturn(Optional.of(envelope));

        // when
        // then
        assertThatThrownBy(() -> envelopeActionService.updateClassificationAndReprocessEnvelope(uuid))
            .isInstanceOf(EnvelopeNotCompletedOrStaleException.class)
            .hasMessageMatching("^(Envelope with id )[\\S]+( is not completed, aborted or stale)$");
    }

    @Test
    void updateClassificationAndReprocessEnvelope_should_throw_exception_if_no_events_for_envelope() {
        // given
        var envelope = envelope(SUPPLEMENTARY_EVIDENCE, NOTIFICATION_SENT, null, null);
        given(processEventRepository.findByZipFileNameOrderByCreatedAtDesc(envelope.getZipFileName()))
            .willReturn(emptyList());

        var uuid = UUID.randomUUID();
        given(envelopeRepository.findById(uuid)).willReturn(Optional.of(envelope));

        // when
        // then
        assertThatThrownBy(() -> envelopeActionService.updateClassificationAndReprocessEnvelope(uuid))
            .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void updateClassificationAndReprocessEnvelope_should_throw_exception_if_envelope_has_uploaded_status() {
        // given
        var uuid = UUID.randomUUID();
        var envelope = envelope(SUPPLEMENTARY_EVIDENCE, UPLOADED, null, null);
        given(envelopeRepository.findById(uuid)).willReturn(Optional.of(envelope));

        // when
        // then
        assertThatThrownBy(() -> envelopeActionService.updateClassificationAndReprocessEnvelope(uuid))
            .isInstanceOf(EnvelopeNotCompletedOrStaleException.class)
            .hasMessageMatching("^(Envelope with id )[\\S]+( is not completed, aborted or stale)$");
    }

    @ParameterizedTest
    @EnumSource(
        value = Classification.class,
        names = {"EXCEPTION", "SUPPLEMENTARY_EVIDENCE_WITH_OCR", "NEW_APPLICATION"}
    )
    void updateClassificationAndReprocessEnvelope_should_throw_exception_if_classification_is_not_expected(
        Classification classification
    ) {
        // given
        var uuid = UUID.randomUUID();
        var envelope = envelope(classification, NOTIFICATION_SENT, null, null);
        given(envelopeRepository.findById(uuid)).willReturn(Optional.of(envelope));

        // when
        // then
        assertThatThrownBy(() -> envelopeActionService.updateClassificationAndReprocessEnvelope(uuid))
            .isInstanceOf(EnvelopeClassificationException.class)
            .hasMessageMatching("^(Envelope with id )[\\S]+( does not have SUPPLEMENTARY_EVIDENCE classification)$");
    }

    @Test
    void updateClassificationAndReprocessEnvelope_should_throw_exception_if_envelope_has_ccdid() {
        // given
        var uuid = UUID.randomUUID();
        var envelope = envelope(SUPPLEMENTARY_EVIDENCE, NOTIFICATION_SENT, "111222333", "create");
        given(envelopeRepository.findById(uuid)).willReturn(Optional.of(envelope));

        // when
        // then
        assertThatThrownBy(() -> envelopeActionService.updateClassificationAndReprocessEnvelope(uuid))
            .isInstanceOf(EnvelopeProcessedInCcdException.class)
            .hasMessageMatching("^(Envelope with id )[\\S]+( has already been processed in CCD)$");
    }

    private Envelope envelope(
        Status status,
        String ccdId,
        String ccdAction
    ) {
        return envelope(SUPPLEMENTARY_EVIDENCE_WITH_OCR, status, ccdId, ccdAction);
    }

    private Envelope envelope(
        Classification classification,
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
            classification,
            emptyList(),
            emptyList(),
            emptyList(),
            "c1",
            null
        );

        envelope.setStatus(status);

        envelope.setCcdId(ccdId);
        envelope.setEnvelopeCcdAction(ccdAction);

        return envelope;
    }
}
