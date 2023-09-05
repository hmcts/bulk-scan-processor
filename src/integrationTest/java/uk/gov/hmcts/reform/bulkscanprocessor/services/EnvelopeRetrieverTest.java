package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.bulkscanprocessor.config.IntegrationTest;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ForbiddenException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.EnvelopeResponse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.EnvelopeCreator.envelope;

@IntegrationTest
public class EnvelopeRetrieverTest {

    @Autowired private EnvelopeRepository envelopeRepo;
    @Mock EnvelopeAccessService accessService;

    private EnvelopeRetrieverService service;

    @BeforeEach
    public void setUp() {
        this.service = new EnvelopeRetrieverService(envelopeRepo, accessService);
    }

    @Disabled
    @Test
    public void should_retrieve_envelopes_by_status_and_jurisdiction() {
        // given
        dbContains(
            envelope("A", Status.UPLOADED),
            envelope("A", Status.UPLOADED),
            envelope("A", Status.NOTIFICATION_SENT),
            envelope("B", Status.UPLOADED),
            envelope("B", Status.NOTIFICATION_SENT)
        );

        // and
        serviceCanReadFromJurisdiction("service_A", "A");

        // when
        List<EnvelopeResponse> envs = service.findByServiceAndStatus("service_A", Status.UPLOADED);

        // then
        assertThat(envs).hasSize(2);
        assertThat(envs).allMatch(e -> e.getJurisdiction().equals("A"));
        assertThat(envs).allMatch(e -> e.getStatus() == Status.UPLOADED);
    }

    @Disabled
    @Test
    public void should_return_empty_list_if_no_envelopes_for_given_jurisdiction_and_status_are_found() {

        // given
        dbContains(
            envelope("A", Status.UPLOADED),
            envelope("A", Status.NOTIFICATION_SENT),
            envelope("B", Status.UPLOADED),
            envelope("B", Status.UPLOADED)
        );

        // and
        serviceCanReadFromJurisdiction("service_B", "B");

        // when
        List<EnvelopeResponse> envs = service.findByServiceAndStatus("service_B", Status.NOTIFICATION_SENT);

        // then
        assertThat(envs).hasSize(0);
    }

    @Disabled
    @Test
    public void should_retrieve_all_envelopes_for_given_jurisdiction_if_passed_status_is_null() {
        // given
        dbContains(
            envelope("X", Status.UPLOAD_FAILURE),
            envelope("X", Status.UPLOAD_FAILURE),
            envelope("X", Status.NOTIFICATION_SENT),
            envelope("X", Status.UPLOADED),
            envelope("Y", Status.NOTIFICATION_SENT)
        );

        // and
        serviceCanReadFromJurisdiction("service_X", "X");

        // when
        List<EnvelopeResponse> envs = service.findByServiceAndStatus("service_X", null);

        // then
        assertThat(envs).hasSize(4);
        assertThat(envs).allMatch(e -> e.getJurisdiction().equals("X"));
    }

    @Disabled
    @Test
    public void should_retrieve_single_envelope_by_id() {
        // given
        Envelope envelopeIdDb = envelopeRepo.saveAndFlush(envelope("X", Status.UPLOADED));
        serviceCanReadFromJurisdiction("service_X", "X");

        // when
        Optional<EnvelopeResponse> foundEnvelope = service.findById("service_X", envelopeIdDb.getId());

        // then
        assertThat(foundEnvelope).map(EnvelopeResponse::getId).get().isEqualTo(envelopeIdDb.getId());
    }

    @Disabled
    @Test
    public void should_return_empty_optional_if_envelope_is_not_found() {
        // given
        envelopeRepo.saveAndFlush(envelope("X", Status.UPLOADED));
        serviceCanReadFromJurisdiction("service_X", "X");

        // when
        Optional<EnvelopeResponse> foundEnvelope = service.findById("service_X", UUID.randomUUID());

        // then
        assertThat(foundEnvelope).isEmpty();
    }

    @Disabled
    @Test
    public void should_throw_an_exception_if_service_cannot_read_existing_envelope() {
        // given
        Envelope envelopeForServiceB = envelopeRepo.saveAndFlush(envelope("B", Status.UPLOADED));
        serviceCanReadFromJurisdiction("service_A", "A");

        // when
        Throwable err = catchThrowable(() -> service.findById("service_A", envelopeForServiceB.getId()));

        // then
        assertThat(err)
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("service_A")
            .hasMessageContaining(envelopeForServiceB.getId().toString());
    }

    @AfterEach
    public void tearDown() throws Exception {
        envelopeRepo.deleteAll();
    }

    private void dbContains(Envelope... envelopes) {
        for (Envelope env : envelopes) {
            envelopeRepo.saveAndFlush(env);
        }
    }

    private void serviceCanReadFromJurisdiction(String serviceName, String jurisdiction) {
        given(this.accessService.getReadJurisdictionForService(serviceName))
            .willReturn(jurisdiction);
    }
}
