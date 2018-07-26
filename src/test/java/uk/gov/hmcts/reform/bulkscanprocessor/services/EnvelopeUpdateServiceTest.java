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

    @Mock private EnvelopeRepository repo;
    @Mock private EnvelopeAccessService accessService;

    private EnvelopeUpdateService service;

    @Before
    public void setUp() throws Exception {
        service = new EnvelopeUpdateService(repo, accessService);
    }

    @Test
    public void markAsConsumed_should_throw_an_exception_if_envelope_with_given_id_does_not_exist() throws Exception {
        //given
        given(repo.findById(any())).willReturn(Optional.empty());

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

        given(repo.findById(any(UUID.class))).willReturn(Optional.of(envelopeInDb));
        given(repo.saveAndFlush(any(Envelope.class))).willReturn(envelopeInDb);

        // when
        service.markAsConsumed(randomUUID(), "some_service");

        // then
        ArgumentCaptor<Envelope> argumentCaptor = ArgumentCaptor.forClass(Envelope.class);
        verify(repo).saveAndFlush(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue().getStatus()).isEqualTo(Event.DOC_CONSUMED);
    }
}
