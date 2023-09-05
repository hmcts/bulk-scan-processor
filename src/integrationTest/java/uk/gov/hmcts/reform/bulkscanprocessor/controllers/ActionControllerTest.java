package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.bulkscanprocessor.config.IntegrationContextInitializer;
import uk.gov.hmcts.reform.bulkscanprocessor.config.IntegrationTest;
import uk.gov.hmcts.reform.bulkscanprocessor.config.Profiles;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;
import uk.gov.hmcts.reform.bulkscanprocessor.services.EnvelopeActionService;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.ABORTED;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.COMPLETED;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.NOTIFICATION_SENT;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.UPLOADED;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification.NEW_APPLICATION;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification.SUPPLEMENTARY_EVIDENCE;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification.SUPPLEMENTARY_EVIDENCE_WITH_OCR;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.DOC_PROCESSED_NOTIFICATION_SENT;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.DOC_UPLOADED;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.MANUAL_RETRIGGER_PROCESSING;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.MANUAL_STATUS_CHANGE;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.ZIPFILE_PROCESSING_STARTED;

@ActiveProfiles({
    IntegrationContextInitializer.PROFILE_WIREMOCK,
    Profiles.SERVICE_BUS_STUB,
    Profiles.STORAGE_STUB
})
@AutoConfigureMockMvc
@IntegrationTest
public class ActionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EnvelopeActionService envelopeActionService;

    @MockBean
    private EnvelopeRepository envelopeRepository;

    @MockBean
    private ProcessEventRepository processEventRepository;

    @AfterEach
    public void cleanUp() {
        processEventRepository.deleteAll();
        envelopeRepository.deleteAll();
    }

    @Disabled
    @Test
    void should_respond_ok_if_envelope_has_notification_sent_status_and_stale_events_for_reprocess()
        throws Exception {

        UUID envelopeId = UUID.randomUUID();

        Envelope envelope = envelope(NOTIFICATION_SENT, null, null);
        Optional<Envelope> envelopeOpt = Optional.of(envelope);
        given(envelopeRepository.findById(envelopeId)).willReturn(envelopeOpt);

        Instant fourtyNineHoursAgo = Instant.now().minus(49, HOURS);
        Instant fiftyHoursAgo = Instant.now().minus(50, HOURS);
        Instant fiftyOneHoursAgo = Instant.now().minus(51, HOURS);
        ProcessEvent event1 = new ProcessEvent();
        event1.setCreatedAt(fourtyNineHoursAgo);
        ProcessEvent event2 = new ProcessEvent();
        event2.setCreatedAt(fiftyHoursAgo);
        ProcessEvent event3 = new ProcessEvent();
        event3.setCreatedAt(fiftyOneHoursAgo);
        given(processEventRepository.findByZipFileNameOrderByCreatedAtDesc(envelope.getZipFileName()))
            .willReturn(asList(event1, event2, event3));

        mockMvc
            .perform(
                put("/actions/reprocess/" + envelopeId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer valid-actions-api-key")
            )
            .andExpect(status().isOk());

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

    @Disabled
    @Test
    void should_respond_conflict_if_envelope_has_notification_sent_status_and_not_stale_events_for_reprocess()
        throws Exception {

        UUID envelopeId = UUID.randomUUID();

        Envelope envelope = envelope(NOTIFICATION_SENT, null, null);
        Optional<Envelope> envelopeOpt = Optional.of(envelope);
        given(envelopeRepository.findById(envelopeId)).willReturn(envelopeOpt);

        Instant fourtySevenHoursAgo = Instant.now().minus(47, HOURS);
        Instant fiftyHoursAgo = Instant.now().minus(50, HOURS);
        Instant fiftyOneHoursAgo = Instant.now().minus(51, HOURS);
        ProcessEvent event1 = new ProcessEvent();
        event1.setCreatedAt(fourtySevenHoursAgo);
        ProcessEvent event2 = new ProcessEvent();
        event2.setCreatedAt(fiftyHoursAgo);
        ProcessEvent event3 = new ProcessEvent();
        event3.setCreatedAt(fiftyOneHoursAgo);
        given(processEventRepository.findByZipFileNameOrderByCreatedAtDesc(envelope.getZipFileName()))
            .willReturn(asList(event1, event2, event3));

        mockMvc
            .perform(
                put("/actions/reprocess/" + envelopeId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer valid-actions-api-key")
            )
            .andExpect(status().isConflict());

        verify(envelopeRepository).findById(envelopeId);
        verifyNoMoreInteractions(envelopeRepository);
        verify(processEventRepository).findByZipFileNameOrderByCreatedAtDesc(envelope.getZipFileName());
        verifyNoMoreInteractions(processEventRepository);
    }

    @Disabled
    @Test
    void should_respond_conflict_if_envelope_has_uploaded_status_for_reprocess() throws Exception {

        UUID envelopeId = UUID.randomUUID();

        Envelope envelope = envelope(UPLOADED, null, null);
        Optional<Envelope> envelopeOpt = Optional.of(envelope);
        given(envelopeRepository.findById(envelopeId)).willReturn(envelopeOpt);

        Instant fourtyNineHoursAgo = Instant.now().minus(49, HOURS);
        Instant fiftyHoursAgo = Instant.now().minus(50, HOURS);
        Instant fiftyOneHoursAgo = Instant.now().minus(51, HOURS);
        ProcessEvent event1 = new ProcessEvent();
        event1.setCreatedAt(fourtyNineHoursAgo);
        ProcessEvent event2 = new ProcessEvent();
        event2.setCreatedAt(fiftyHoursAgo);
        ProcessEvent event3 = new ProcessEvent();
        event3.setCreatedAt(fiftyOneHoursAgo);
        given(processEventRepository.findByZipFileNameOrderByCreatedAtDesc(envelope.getZipFileName()))
            .willReturn(asList(event1, event2, event3));

        mockMvc
            .perform(
                put("/actions/reprocess/" + envelopeId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer valid-actions-api-key")
            )
            .andExpect(status().isConflict());

        verify(envelopeRepository).findById(envelopeId);
        verifyNoMoreInteractions(envelopeRepository);
    }

    @Disabled
    @Test
    void should_respond_conflict_if_envelope_already_processed_for_reprocess() throws Exception {

        UUID envelopeId = UUID.randomUUID();

        Envelope envelope = envelope(COMPLETED, "111222333", "created");
        Optional<Envelope> envelopeOpt = Optional.of(envelope);
        given(envelopeRepository.findById(envelopeId)).willReturn(envelopeOpt);

        Instant fourtyNineHoursAgo = Instant.now().minus(49, HOURS);
        Instant fiftyHoursAgo = Instant.now().minus(50, HOURS);
        Instant fiftyOneHoursAgo = Instant.now().minus(51, HOURS);
        ProcessEvent event1 = new ProcessEvent();
        event1.setCreatedAt(fourtyNineHoursAgo);
        ProcessEvent event2 = new ProcessEvent();
        event2.setCreatedAt(fiftyHoursAgo);
        ProcessEvent event3 = new ProcessEvent();
        event3.setCreatedAt(fiftyOneHoursAgo);
        given(processEventRepository.findByZipFileNameOrderByCreatedAtDesc(envelope.getZipFileName()))
            .willReturn(asList(event1, event2, event3));

        mockMvc
            .perform(
                put("/actions/reprocess/" + envelopeId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer valid-actions-api-key")
            )
            .andExpect(status().isConflict());

        verify(envelopeRepository).findById(envelopeId);
        verifyNoMoreInteractions(envelopeRepository);
    }

    @Disabled
    @Test
    void should_respond_conflict_if_envelope_already_aborted_for_reprocess() throws Exception {

        UUID envelopeId = UUID.randomUUID();

        Envelope envelope = envelope(ABORTED, "111222333", "created");
        Optional<Envelope> envelopeOpt = Optional.of(envelope);
        given(envelopeRepository.findById(envelopeId)).willReturn(envelopeOpt);

        Instant fourtyNineHoursAgo = Instant.now().minus(49, HOURS);
        Instant fiftyHoursAgo = Instant.now().minus(50, HOURS);
        Instant fiftyOneHoursAgo = Instant.now().minus(51, HOURS);
        ProcessEvent event1 = new ProcessEvent();
        event1.setCreatedAt(fourtyNineHoursAgo);
        ProcessEvent event2 = new ProcessEvent();
        event2.setCreatedAt(fiftyHoursAgo);
        ProcessEvent event3 = new ProcessEvent();
        event3.setCreatedAt(fiftyOneHoursAgo);
        given(processEventRepository.findByZipFileNameOrderByCreatedAtDesc(envelope.getZipFileName()))
            .willReturn(asList(event1, event2, event3));

        mockMvc
            .perform(
                put("/actions/reprocess/" + envelopeId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer valid-actions-api-key")
            )
            .andExpect(status().isConflict());

        verify(envelopeRepository).findById(envelopeId);
        verifyNoMoreInteractions(envelopeRepository);
    }

    @Disabled
    @Test
    void should_respond_bad_request_if_uuid_corrupted_for_reprocess() throws Exception {

        mockMvc
            .perform(
                put("/actions/reprocess/" + "corrupted")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer valid-actions-api-key")
            )
            .andExpect(status().isBadRequest());

        verifyNoInteractions(envelopeRepository);
        verifyNoInteractions(processEventRepository);
    }

    @Disabled
    @Test
    void should_return_unauthorized_when_authorisation_header_is_invalid_for_reprocess() throws Exception {
        // given
        UUID envelopeId = UUID.randomUUID();

        // when
        mockMvc
            .perform(
                put("/actions/reprocess/" + envelopeId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-api-key")
            )
            .andDo(print())
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Invalid API Key"));
    }

    @Disabled
    @Test
    void should_return_unauthorized_when_authorisation_header_is_missing_bearer_prefix_for_reprocess()
            throws Exception {
        // given
        UUID envelopeId = UUID.randomUUID();

        // when
        mockMvc
            .perform(
                put("/actions/reprocess/" + envelopeId)
                    .header(HttpHeaders.AUTHORIZATION, "valid-report-api-key")
            )
            .andDo(print())
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Invalid API Key"));
    }

    @Disabled
    @Test
    void should_return_unauthorized_when_authorisation_header_is_missing_for_reprocess() throws Exception {
        // given
        UUID envelopeId = UUID.randomUUID();

        // when
        mockMvc
            .perform(
                put("/actions/reprocess/" + envelopeId)
            )
            .andDo(print())
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("API Key is missing"));
    }

    @Disabled
    @Test
    void should_respond_ok_if_envelope_has_notification_sent_status_and_stale_events_for_classification_update()
        throws Exception {

        UUID envelopeId = UUID.randomUUID();

        Envelope envelope = envelope(SUPPLEMENTARY_EVIDENCE, NOTIFICATION_SENT, null, null);
        Optional<Envelope> envelopeOpt = Optional.of(envelope);
        given(envelopeRepository.findById(envelopeId)).willReturn(envelopeOpt);

        Instant fortyNineHoursAgo = Instant.now().minus(49, HOURS);
        Instant fiftyHoursAgo = Instant.now().minus(50, HOURS);
        Instant fiftyOneHoursAgo = Instant.now().minus(51, HOURS);
        ProcessEvent event1 = new ProcessEvent();
        event1.setCreatedAt(fortyNineHoursAgo);
        ProcessEvent event2 = new ProcessEvent();
        event2.setCreatedAt(fiftyHoursAgo);
        ProcessEvent event3 = new ProcessEvent();
        event3.setCreatedAt(fiftyOneHoursAgo);
        given(processEventRepository.findByZipFileNameOrderByCreatedAtDesc(envelope.getZipFileName()))
            .willReturn(asList(event1, event2, event3));

        mockMvc
            .perform(
                put("/actions/update-classification-reprocess/" + envelopeId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer valid-actions-api-key")
            )
            .andExpect(status().isOk());

        var processEventCaptor = ArgumentCaptor.forClass(ProcessEvent.class);
        verify(processEventRepository).save(processEventCaptor.capture());
        assertThat(processEventCaptor.getValue().getContainer())
            .isEqualTo(envelope.getContainer());
        assertThat(processEventCaptor.getValue().getZipFileName())
            .isEqualTo(envelope.getZipFileName());
        assertThat(processEventCaptor.getValue().getEvent()).isEqualTo(MANUAL_RETRIGGER_PROCESSING);
        assertThat(processEventCaptor.getValue().getReason())
            .isEqualTo(
                "Updated envelope classification to EXCEPTION and status to UPLOADED "
                    + "to create Exception Record for the envelope"
            );

        verify(envelopeRepository).updateEnvelopeClassificationAndStatus(envelopeId, envelope.getContainer());
    }

    @Disabled
    @Test
    void should_respond_conflict_if_envelope_status_notification_sent_and_not_stale_events_for_classification_change()
        throws Exception {

        UUID envelopeId = UUID.randomUUID();

        Envelope envelope = envelope(SUPPLEMENTARY_EVIDENCE, NOTIFICATION_SENT, null, null);
        Optional<Envelope> envelopeOpt = Optional.of(envelope);
        given(envelopeRepository.findById(envelopeId)).willReturn(envelopeOpt);

        Instant fourtySevenHoursAgo = Instant.now().minus(47, HOURS);
        Instant fiftyHoursAgo = Instant.now().minus(50, HOURS);
        Instant fiftyOneHoursAgo = Instant.now().minus(51, HOURS);
        ProcessEvent event1 = new ProcessEvent();
        event1.setCreatedAt(fourtySevenHoursAgo);
        ProcessEvent event2 = new ProcessEvent();
        event2.setCreatedAt(fiftyHoursAgo);
        ProcessEvent event3 = new ProcessEvent();
        event3.setCreatedAt(fiftyOneHoursAgo);
        given(processEventRepository.findByZipFileNameOrderByCreatedAtDesc(envelope.getZipFileName()))
            .willReturn(asList(event1, event2, event3));

        mockMvc
            .perform(
                put("/actions/update-classification-reprocess/" + envelopeId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer valid-actions-api-key")
            )
            .andExpect(status().isConflict());

        verify(envelopeRepository).findById(envelopeId);
        verifyNoMoreInteractions(envelopeRepository);
        verify(processEventRepository).findByZipFileNameOrderByCreatedAtDesc(envelope.getZipFileName());
        verifyNoMoreInteractions(processEventRepository);
    }

    @Disabled
    @Test
    void should_respond_conflict_if_envelope_has_uploaded_status_for_classification_update() throws Exception {

        UUID envelopeId = UUID.randomUUID();

        Envelope envelope = envelope(SUPPLEMENTARY_EVIDENCE, UPLOADED, null, null);
        Optional<Envelope> envelopeOpt = Optional.of(envelope);
        given(envelopeRepository.findById(envelopeId)).willReturn(envelopeOpt);

        Instant fortyNineHoursAgo = Instant.now().minus(49, HOURS);
        Instant fiftyHoursAgo = Instant.now().minus(50, HOURS);
        Instant fiftyOneHoursAgo = Instant.now().minus(51, HOURS);
        ProcessEvent event1 = new ProcessEvent();
        event1.setCreatedAt(fortyNineHoursAgo);
        ProcessEvent event2 = new ProcessEvent();
        event2.setCreatedAt(fiftyHoursAgo);
        ProcessEvent event3 = new ProcessEvent();
        event3.setCreatedAt(fiftyOneHoursAgo);
        given(processEventRepository.findByZipFileNameOrderByCreatedAtDesc(envelope.getZipFileName()))
            .willReturn(asList(event1, event2, event3));

        mockMvc
            .perform(
                put("/actions/update-classification-reprocess/" + envelopeId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer valid-actions-api-key")
            )
            .andExpect(status().isConflict());

        verify(envelopeRepository).findById(envelopeId);
        verifyNoMoreInteractions(envelopeRepository);
    }

    @Disabled
    @Test
    void should_respond_conflict_if_envelope_already_processed_for_classification_update() throws Exception {

        UUID envelopeId = UUID.randomUUID();

        Envelope envelope = envelope(SUPPLEMENTARY_EVIDENCE, COMPLETED, "111222333", "created");
        Optional<Envelope> envelopeOpt = Optional.of(envelope);
        given(envelopeRepository.findById(envelopeId)).willReturn(envelopeOpt);

        Instant fortyNineHoursAgo = Instant.now().minus(49, HOURS);
        Instant fiftyHoursAgo = Instant.now().minus(50, HOURS);
        Instant fiftyOneHoursAgo = Instant.now().minus(51, HOURS);
        ProcessEvent event1 = new ProcessEvent();
        event1.setCreatedAt(fortyNineHoursAgo);
        ProcessEvent event2 = new ProcessEvent();
        event2.setCreatedAt(fiftyHoursAgo);
        ProcessEvent event3 = new ProcessEvent();
        event3.setCreatedAt(fiftyOneHoursAgo);
        given(processEventRepository.findByZipFileNameOrderByCreatedAtDesc(envelope.getZipFileName()))
            .willReturn(asList(event1, event2, event3));

        mockMvc
            .perform(
                put("/actions/update-classification-reprocess/" + envelopeId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer valid-actions-api-key")
            )
            .andExpect(status().isConflict());

        verify(envelopeRepository).findById(envelopeId);
        verifyNoMoreInteractions(envelopeRepository);
    }

    @Disabled
    @Test
    void should_respond_conflict_if_envelope_already_aborted_for_classification_update() throws Exception {

        UUID envelopeId = UUID.randomUUID();

        Envelope envelope = envelope(SUPPLEMENTARY_EVIDENCE, ABORTED, "111222333", "created");
        Optional<Envelope> envelopeOpt = Optional.of(envelope);
        given(envelopeRepository.findById(envelopeId)).willReturn(envelopeOpt);

        Instant fortyNineHoursAgo = Instant.now().minus(49, HOURS);
        Instant fiftyHoursAgo = Instant.now().minus(50, HOURS);
        Instant fiftyOneHoursAgo = Instant.now().minus(51, HOURS);
        ProcessEvent event1 = new ProcessEvent();
        event1.setCreatedAt(fortyNineHoursAgo);
        ProcessEvent event2 = new ProcessEvent();
        event2.setCreatedAt(fiftyHoursAgo);
        ProcessEvent event3 = new ProcessEvent();
        event3.setCreatedAt(fiftyOneHoursAgo);
        given(processEventRepository.findByZipFileNameOrderByCreatedAtDesc(envelope.getZipFileName()))
            .willReturn(asList(event1, event2, event3));

        mockMvc
            .perform(
                put("/actions/update-classification-reprocess/" + envelopeId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer valid-actions-api-key")
            )
            .andExpect(status().isConflict());

        verify(envelopeRepository).findById(envelopeId);
        verifyNoMoreInteractions(envelopeRepository);
    }

    @Disabled
    @Test
    void should_respond_conflict_if_envelope_classification_is_not_supplementary_evidence_for_classification_update()
        throws Exception {

        UUID envelopeId = UUID.randomUUID();

        Envelope envelope = envelope(NEW_APPLICATION, NOTIFICATION_SENT, null, null);
        Optional<Envelope> envelopeOpt = Optional.of(envelope);
        given(envelopeRepository.findById(envelopeId)).willReturn(envelopeOpt);

        mockMvc
            .perform(
                put("/actions/update-classification-reprocess/" + envelopeId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer valid-actions-api-key")
            )
            .andExpect(status().isConflict());

        verify(envelopeRepository).findById(envelopeId);
        verifyNoMoreInteractions(envelopeRepository);
    }

    @Disabled
    @Test
    void should_respond_bad_request_if_uuid_corrupted_for_classification_update() throws Exception {

        mockMvc
            .perform(
                put("/actions/update-classification-reprocess/" + "corrupted")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer valid-actions-api-key")
            )
            .andExpect(status().isBadRequest());

        verifyNoInteractions(envelopeRepository);
        verifyNoInteractions(processEventRepository);
    }

    @Disabled
    @Test
    void should_return_unauthorized_when_authorisation_header_is_invalid_for_classification_update()
        throws Exception {
        // given
        UUID envelopeId = UUID.randomUUID();

        // when
        mockMvc
            .perform(
                put("/actions/update-classification-reprocess/" + envelopeId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-api-key")
            )
            .andDo(print())
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Invalid API Key"));
    }

    @Disabled
    @Test
    void should_return_unauthorized_when_authorisation_header_is_missing_bearer_prefix_for_classification_update()
        throws Exception {
        // given
        UUID envelopeId = UUID.randomUUID();

        // when
        mockMvc
            .perform(
                put("/actions/update-classification-reprocess/" + envelopeId)
                    .header(HttpHeaders.AUTHORIZATION, "valid-report-api-key")
            )
            .andDo(print())
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Invalid API Key"));
    }

    @Disabled
    @Test
    void should_return_unauthorized_when_authorisation_header_is_missing_for_classification_update()
        throws Exception {
        // given
        UUID envelopeId = UUID.randomUUID();

        // when
        mockMvc
            .perform(
                put("/actions/update-classification-reprocess/" + envelopeId)
            )
            .andDo(print())
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("API Key is missing"));
    }

    @Disabled
    @Test
    void should_respond_ok_if_envelope_has_notification_sent_status_and_stale_events_when_complete()
        throws Exception {

        UUID envelopeId = UUID.randomUUID();

        Envelope envelope = envelope(NOTIFICATION_SENT, null, null);
        Optional<Envelope> envelopeOpt = Optional.of(envelope);
        given(envelopeRepository.findById(envelopeId)).willReturn(envelopeOpt);

        Instant oneHourAgo = Instant.now().minus(1, HOURS);
        Instant twoHoursAgo = Instant.now().minus(2, HOURS);
        Instant threeHoursAgo = Instant.now().minus(3, HOURS);
        Instant fourOneHoursAgo = Instant.now().minus(4, HOURS);
        ProcessEvent event1 = createProcessEvent(envelope, ZIPFILE_PROCESSING_STARTED, fourOneHoursAgo);
        ProcessEvent event2 = createProcessEvent(envelope, DOC_UPLOADED, threeHoursAgo);
        ProcessEvent event3 = createProcessEvent(envelope, Event.COMPLETED, twoHoursAgo);
        ProcessEvent event4 = createProcessEvent(envelope, DOC_PROCESSED_NOTIFICATION_SENT, oneHourAgo);
        given(processEventRepository.findByZipFileNameOrderByCreatedAtDesc(envelope.getZipFileName()))
            .willReturn(asList(event1, event2, event3, event4));

        mockMvc
            .perform(
                put("/actions/" + envelopeId + "/complete")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer valid-actions-api-key")
            )
            .andExpect(status().isOk());

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

    @Disabled
    @Test
    void should_respond_conflict_if_envelope_has_completed_status_for_complete() throws Exception {

        UUID envelopeId = UUID.randomUUID();

        Envelope envelope = envelope(COMPLETED, null, null);
        Optional<Envelope> envelopeOpt = Optional.of(envelope);
        given(envelopeRepository.findById(envelopeId)).willReturn(envelopeOpt);

        Instant oneHourAgo = Instant.now().minus(1, HOURS);
        Instant twoHoursAgo = Instant.now().minus(2, HOURS);
        Instant threeHoursAgo = Instant.now().minus(3, HOURS);
        Instant fourHoursAgo = Instant.now().minus(4, HOURS);
        ProcessEvent event1 = createProcessEvent(envelope, ZIPFILE_PROCESSING_STARTED, fourHoursAgo);
        ProcessEvent event2 = createProcessEvent(envelope, DOC_UPLOADED, threeHoursAgo);
        ProcessEvent event3 = createProcessEvent(envelope, DOC_PROCESSED_NOTIFICATION_SENT, twoHoursAgo);
        ProcessEvent event4 = createProcessEvent(envelope, Event.COMPLETED, oneHourAgo);
        given(processEventRepository.findByZipFileNameOrderByCreatedAtDesc(envelope.getZipFileName()))
            .willReturn(asList(event1, event2, event3, event4));

        mockMvc
            .perform(
                put("/actions/" + envelopeId + "/complete")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer valid-actions-api-key")
            )
            .andExpect(status().isConflict());

        verify(envelopeRepository).findById(envelopeId);
        verifyNoMoreInteractions(envelopeRepository);
    }

    @Disabled
    @Test
    void should_respond_conflict_if_envelope_has_no_completed_event_for_complete() throws Exception {

        UUID envelopeId = UUID.randomUUID();

        Envelope envelope = envelope(NOTIFICATION_SENT, null, null);
        Optional<Envelope> envelopeOpt = Optional.of(envelope);
        given(envelopeRepository.findById(envelopeId)).willReturn(envelopeOpt);

        Instant oneHourAgo = Instant.now().minus(1, HOURS);
        Instant twoHoursAgo = Instant.now().minus(2, HOURS);
        Instant threeHoursAgo = Instant.now().minus(3, HOURS);
        ProcessEvent event1 = createProcessEvent(envelope, ZIPFILE_PROCESSING_STARTED, threeHoursAgo);
        ProcessEvent event2 = createProcessEvent(envelope, DOC_UPLOADED, twoHoursAgo);
        ProcessEvent event3 = createProcessEvent(envelope, DOC_PROCESSED_NOTIFICATION_SENT, oneHourAgo);
        given(processEventRepository.findByZipFileNameOrderByCreatedAtDesc(envelope.getZipFileName()))
            .willReturn(asList(event1, event2, event3));

        mockMvc
            .perform(
                put("/actions/" + envelopeId + "/complete")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer valid-actions-api-key")
            )
            .andExpect(status().isConflict());

        verify(envelopeRepository).findById(envelopeId);
        verifyNoMoreInteractions(envelopeRepository);
    }

    @Disabled
    @Test
    void should_respond_bad_request_if_uuid_corrupted_for_complete() throws Exception {

        mockMvc
            .perform(
                put("/actions/" + "corrupted" + "/complete")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer valid-actions-api-key")
            )
            .andExpect(status().isBadRequest());

        verifyNoInteractions(envelopeRepository);
        verifyNoInteractions(processEventRepository);
    }

    @Disabled
    @Test
    void should_return_unauthorized_when_authorisation_header_is_invalid_for_complete() throws Exception {
        // given
        UUID envelopeId = UUID.randomUUID();

        // when
        mockMvc
            .perform(
                put("/actions/" + envelopeId + "/complete")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-api-key")
            )
            .andDo(print())
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Invalid API Key"));
    }

    @Disabled
    @Test
    void should_return_unauthorized_when_authorisation_header_is_missing_bearer_prefix_for_complete()
            throws Exception {
        // given
        UUID envelopeId = UUID.randomUUID();

        // when
        mockMvc
            .perform(
                put("/actions/" + envelopeId + "/complete")
                    .header(HttpHeaders.AUTHORIZATION, "valid-report-api-key")
            )
            .andDo(print())
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Invalid API Key"));
    }

    @Disabled
    @Test
    void should_return_unauthorized_when_authorisation_header_is_missing_for_complete() throws Exception {
        // given
        UUID envelopeId = UUID.randomUUID();

        // when
        mockMvc
            .perform(
                put("/actions/" + envelopeId + "/complete")
            )
            .andDo(print())
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("API Key is missing"));
    }

    @Disabled
    @Test
    void should_respond_ok_if_envelope_has_notification_sent_status_and_stale_events_for_abort()
            throws Exception {

        UUID envelopeId = UUID.randomUUID();

        Envelope envelope = envelope(NOTIFICATION_SENT, null, null);
        Optional<Envelope> envelopeOpt = Optional.of(envelope);
        given(envelopeRepository.findById(envelopeId)).willReturn(envelopeOpt);

        Instant fourtyNineHoursAgo = Instant.now().minus(49, HOURS);
        Instant fiftyHoursAgo = Instant.now().minus(50, HOURS);
        Instant fiftyOneHoursAgo = Instant.now().minus(51, HOURS);
        ProcessEvent event1 = new ProcessEvent();
        event1.setCreatedAt(fourtyNineHoursAgo);
        ProcessEvent event2 = new ProcessEvent();
        event2.setCreatedAt(fiftyHoursAgo);
        ProcessEvent event3 = new ProcessEvent();
        event3.setCreatedAt(fiftyOneHoursAgo);
        given(processEventRepository.findByZipFileNameOrderByCreatedAtDesc(envelope.getZipFileName()))
                .willReturn(asList(event1, event2, event3));

        mockMvc
                .perform(
                    put("/actions/" + envelopeId + "/abort")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-actions-api-key")
                )
            .andExpect(status().isOk());

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

    @Disabled
    @Test
    void should_respond_conflict_if_envelope_has_notification_sent_status_and_not_stale_events_for_abort()
            throws Exception {

        UUID envelopeId = UUID.randomUUID();

        Envelope envelope = envelope(NOTIFICATION_SENT, null, null);
        Optional<Envelope> envelopeOpt = Optional.of(envelope);
        given(envelopeRepository.findById(envelopeId)).willReturn(envelopeOpt);

        Instant fourtySevenHoursAgo = Instant.now().minus(47, HOURS);
        Instant fiftyHoursAgo = Instant.now().minus(50, HOURS);
        Instant fiftyOneHoursAgo = Instant.now().minus(51, HOURS);
        ProcessEvent event1 = new ProcessEvent();
        event1.setCreatedAt(fourtySevenHoursAgo);
        ProcessEvent event2 = new ProcessEvent();
        event2.setCreatedAt(fiftyHoursAgo);
        ProcessEvent event3 = new ProcessEvent();
        event3.setCreatedAt(fiftyOneHoursAgo);
        given(processEventRepository.findByZipFileNameOrderByCreatedAtDesc(envelope.getZipFileName()))
                .willReturn(asList(event1, event2, event3));

        mockMvc
                .perform(
                    put("/actions/" + envelopeId + "/abort")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-actions-api-key")
                )
                .andExpect(status().isConflict());

        verify(envelopeRepository).findById(envelopeId);
        verifyNoMoreInteractions(envelopeRepository);
        verify(processEventRepository).findByZipFileNameOrderByCreatedAtDesc(envelope.getZipFileName());
        verifyNoMoreInteractions(processEventRepository);
    }

    @Disabled
    @Test
    void should_respond_conflict_if_envelope_has_uploaded_status_for_abort() throws Exception {

        UUID envelopeId = UUID.randomUUID();

        Envelope envelope = envelope(UPLOADED, null, null);
        Optional<Envelope> envelopeOpt = Optional.of(envelope);
        given(envelopeRepository.findById(envelopeId)).willReturn(envelopeOpt);

        Instant fourtyNineHoursAgo = Instant.now().minus(49, HOURS);
        Instant fiftyHoursAgo = Instant.now().minus(50, HOURS);
        Instant fiftyOneHoursAgo = Instant.now().minus(51, HOURS);
        ProcessEvent event1 = new ProcessEvent();
        event1.setCreatedAt(fourtyNineHoursAgo);
        ProcessEvent event2 = new ProcessEvent();
        event2.setCreatedAt(fiftyHoursAgo);
        ProcessEvent event3 = new ProcessEvent();
        event3.setCreatedAt(fiftyOneHoursAgo);
        given(processEventRepository.findByZipFileNameOrderByCreatedAtDesc(envelope.getZipFileName()))
                .willReturn(asList(event1, event2, event3));

        mockMvc
                .perform(
                    put("/actions/" + envelopeId + "/abort")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-actions-api-key")
                )
                .andExpect(status().isConflict());

        verify(envelopeRepository).findById(envelopeId);
        verifyNoMoreInteractions(envelopeRepository);
    }

    @Disabled
    @Test
    void should_respond_conflict_if_envelope_already_processed_for_abort() throws Exception {

        UUID envelopeId = UUID.randomUUID();

        Envelope envelope = envelope(COMPLETED, "111222333", "created");
        Optional<Envelope> envelopeOpt = Optional.of(envelope);
        given(envelopeRepository.findById(envelopeId)).willReturn(envelopeOpt);

        Instant fourtyNineHoursAgo = Instant.now().minus(49, HOURS);
        Instant fiftyHoursAgo = Instant.now().minus(50, HOURS);
        Instant fiftyOneHoursAgo = Instant.now().minus(51, HOURS);
        ProcessEvent event1 = new ProcessEvent();
        event1.setCreatedAt(fourtyNineHoursAgo);
        ProcessEvent event2 = new ProcessEvent();
        event2.setCreatedAt(fiftyHoursAgo);
        ProcessEvent event3 = new ProcessEvent();
        event3.setCreatedAt(fiftyOneHoursAgo);
        given(processEventRepository.findByZipFileNameOrderByCreatedAtDesc(envelope.getZipFileName()))
                .willReturn(asList(event1, event2, event3));

        mockMvc
                .perform(
                        put("/actions/" + envelopeId + "/abort")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer valid-actions-api-key")
                )
                .andExpect(status().isConflict());

        verify(envelopeRepository).findById(envelopeId);
        verifyNoMoreInteractions(envelopeRepository);
    }

    @Disabled
    @Test
    void should_respond_conflict_if_envelope_already_aborted_for_abort() throws Exception {

        UUID envelopeId = UUID.randomUUID();

        Envelope envelope = envelope(ABORTED, "111222333", "created");
        Optional<Envelope> envelopeOpt = Optional.of(envelope);
        given(envelopeRepository.findById(envelopeId)).willReturn(envelopeOpt);

        Instant fourtyNineHoursAgo = Instant.now().minus(49, HOURS);
        Instant fiftyHoursAgo = Instant.now().minus(50, HOURS);
        Instant fiftyOneHoursAgo = Instant.now().minus(51, HOURS);
        ProcessEvent event1 = new ProcessEvent();
        event1.setCreatedAt(fourtyNineHoursAgo);
        ProcessEvent event2 = new ProcessEvent();
        event2.setCreatedAt(fiftyHoursAgo);
        ProcessEvent event3 = new ProcessEvent();
        event3.setCreatedAt(fiftyOneHoursAgo);
        given(processEventRepository.findByZipFileNameOrderByCreatedAtDesc(envelope.getZipFileName()))
                .willReturn(asList(event1, event2, event3));

        mockMvc
                .perform(
                    put("/actions/" + envelopeId + "/abort")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-actions-api-key")
            )
            .andExpect(status().isConflict());

        verify(envelopeRepository).findById(envelopeId);
        verifyNoMoreInteractions(envelopeRepository);
    }

    @Disabled
    @Test
    void should_respond_bad_request_if_uuid_corrupted_for_abort() throws Exception {

        mockMvc
            .perform(
                put("/actions/" + "corrupted" + "/abort")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer valid-actions-api-key")
            )
            .andExpect(status().isBadRequest());

        verifyNoInteractions(envelopeRepository);
        verifyNoInteractions(processEventRepository);
    }

    @Disabled
    @Test
    void should_return_unauthorized_when_authorisation_header_is_invalid_for_abort() throws Exception {
        // given
        UUID envelopeId = UUID.randomUUID();

        // when
        mockMvc
            .perform(
                put("/actions/" + envelopeId + "/abort")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-api-key")
            )
            .andDo(print())
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Invalid API Key"));
    }

    @Disabled
    @Test
    void should_return_unauthorized_when_authorisation_header_is_missing_bearer_prefix_for_abort()
            throws Exception {
        // given
        UUID envelopeId = UUID.randomUUID();

        // when
        mockMvc
            .perform(
                put("/actions/" + envelopeId + "/abort")
                    .header(HttpHeaders.AUTHORIZATION, "valid-report-api-key")
            )
            .andDo(print())
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Invalid API Key"));
    }

    @Disabled
    @Test
    void should_return_unauthorized_when_authorisation_header_is_missing_for_abort() throws Exception {
        // given
        UUID envelopeId = UUID.randomUUID();

        // when
        mockMvc
            .perform(
                put("/actions/" + envelopeId + "/abort")
            )
            .andDo(print())
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("API Key is missing"));
    }

    private ProcessEvent createProcessEvent(Envelope envelope, Event completed, Instant timestamp) {
        ProcessEvent event = new ProcessEvent(
            envelope.getContainer(),
            envelope.getZipFileName(),
            completed
        );
        event.setCreatedAt(timestamp);
        return event;
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
            "container",
            null
        );

        envelope.setStatus(status);

        envelope.setCcdId(ccdId);
        envelope.setEnvelopeCcdAction(ccdAction);

        return envelope;
    }
}
