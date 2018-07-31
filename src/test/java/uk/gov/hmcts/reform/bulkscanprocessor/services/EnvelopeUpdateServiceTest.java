package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Event;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EnvelopeNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.helper.EnvelopeCreator;

import java.util.Optional;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.CONSUMED;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.UPLOAD_FAILURE;

@SuppressWarnings({"PMD.BeanMembersShouldSerialize", "checkstyle:linelength"})
@RunWith(MockitoJUnitRunner.class)
public class EnvelopeUpdateServiceTest {

    @Mock private EnvelopeRepository envelopeRepo;
    @Mock private ProcessEventRepository eventRepo;
    @Mock private EnvelopeAccessService accessService;
    @Mock private EnvelopeStatusChangeValidator statusChangeValidator;

    private EnvelopeUpdateService service;

    @Before
    public void setUp() throws Exception {
        service = new EnvelopeUpdateService(envelopeRepo, eventRepo, accessService, statusChangeValidator);
    }

    @Test
    public void updateStatus_should_throw_an_exception_if_envelope_with_given_id_does_not_exist() throws Exception {
        //given
        given(envelopeRepo.findById(any())).willReturn(Optional.empty());

        // when
        Throwable exc = catchThrowable(() -> service.updateStatus(randomUUID(), CONSUMED, "some_service"));

        // then
        assertThat(exc)
            .isNotNull()
            .isInstanceOf(EnvelopeNotFoundException.class);
    }

    @Test
    public void updateStatus_should_set_appropriate_status_on_envelope_if_it_exists() throws Exception {
        //given
        Envelope envelopeInDb = EnvelopeCreator.envelope();

        given(envelopeRepo.findById(any(UUID.class))).willReturn(Optional.of(envelopeInDb));

        // when
        service.updateStatus(randomUUID(), CONSUMED, "some_service");

        // then status should be updated
        ArgumentCaptor<Envelope> envelopeParam = ArgumentCaptor.forClass(Envelope.class);
        verify(envelopeRepo).save(envelopeParam.capture());
        assertThat(envelopeParam.getValue().getStatus()).isEqualTo(CONSUMED);
    }

    @Test
    public void updateStatus_should_create_an_event_if_there_is_one_configured_for_the_new_status() throws Exception {
        //given
        Envelope envelopeInDb = EnvelopeCreator.envelope();

        given(envelopeRepo.findById(any(UUID.class))).willReturn(Optional.of(envelopeInDb));

        // when
        service.updateStatus(randomUUID(), CONSUMED, "some_service");

        // then
        ArgumentCaptor<ProcessEvent> eventParam = ArgumentCaptor.forClass(ProcessEvent.class);
        verify(eventRepo).save(eventParam.capture());
        assertThat(eventParam.getValue().getEvent()).isEqualTo(Event.DOC_CONSUMED);
    }

    @Test
    public void updateStatus_should_not_create_an_event_if_there_is_none_configured_for_the_new_status() throws Exception {
        //given
        Envelope envelopeInDb = EnvelopeCreator.envelope();

        given(envelopeRepo.findById(any(UUID.class))).willReturn(Optional.of(envelopeInDb));

        // when
        service.updateStatus(randomUUID(), UPLOAD_FAILURE, "some_service");

        // then
        verify(eventRepo, never()).save(any());
    }
}
