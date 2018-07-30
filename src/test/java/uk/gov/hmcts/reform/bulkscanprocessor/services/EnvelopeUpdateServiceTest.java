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
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EnvelopeNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.helper.EnvelopeCreator;

import java.util.Optional;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@SuppressWarnings("PMD.BeanMembersShouldSerialize")
@RunWith(MockitoJUnitRunner.class)
public class EnvelopeUpdateServiceTest {

    @Mock private EnvelopeRepository envelopeRepo;
    @Mock private ProcessEventRepository eventRepo;
    @Mock private EnvelopeAccessService accessService;

    private EnvelopeUpdateService service;

    @Before
    public void setUp() throws Exception {
        service = new EnvelopeUpdateService(envelopeRepo, eventRepo, accessService);
    }

    @Test
    public void markAsConsumed_should_throw_an_exception_if_envelope_with_given_id_does_not_exist() throws Exception {
        //given
        given(envelopeRepo.findById(any())).willReturn(Optional.empty());

        // when
        Throwable exc = catchThrowable(() -> service.markAsConsumed(randomUUID(), "some_service"));

        // then
        assertThat(exc)
            .isNotNull()
            .isInstanceOf(EnvelopeNotFoundException.class);
    }

    @Test
    public void markAsConsumed_should_set_appropriate_status_on_envelope_if_it_exists() throws Exception {
        //given
        Envelope envelopeInDb = EnvelopeCreator.envelope();

        given(envelopeRepo.findById(any(UUID.class))).willReturn(Optional.of(envelopeInDb));

        // when
        service.markAsConsumed(randomUUID(), "some_service");

        // then status should be updated
        ArgumentCaptor<Envelope> envelopeParam = ArgumentCaptor.forClass(Envelope.class);
        verify(envelopeRepo).save(envelopeParam.capture());
        assertThat(envelopeParam.getValue().getStatus()).isEqualTo(Status.CONSUMED);

        // and event should be created
        ArgumentCaptor<ProcessEvent> eventParam = ArgumentCaptor.forClass(ProcessEvent.class);
        verify(eventRepo).save(eventParam.capture());
        assertThat(eventParam.getValue().getEvent()).isEqualTo(Event.DOC_CONSUMED);
    }
}
