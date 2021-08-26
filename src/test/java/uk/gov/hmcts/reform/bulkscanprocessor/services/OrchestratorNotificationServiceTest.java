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
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.InvalidMessageException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.EnvelopeMsg;
import uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.ServiceBusSendHelper;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.NOTIFICATION_SENT;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.EnvelopeCreator.envelope;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.DOC_PROCESSED_NOTIFICATION_SENT;

@ExtendWith(MockitoExtension.class)
class OrchestratorNotificationServiceTest {
    private OrchestratorNotificationService orchestratorNotificationService;

    @Mock
    private ServiceBusSendHelper serviceBusHelper;

    @Mock
    private EnvelopeRepository envelopeRepo;

    @Mock
    private ProcessEventRepository processEventRepo;

    private AtomicInteger successCount;

    @BeforeEach
    void setUp() {
        orchestratorNotificationService = new OrchestratorNotificationService(
            serviceBusHelper,
            envelopeRepo,
            processEventRepo
        );
        successCount = new AtomicInteger(0);
    }

    @Test
    void should_notify_orchestrator() {
        // given
        Envelope env = envelope();

        // when
        orchestratorNotificationService.processEnvelope(successCount, env);

        // then
        verify(serviceBusHelper).sendMessage(any(EnvelopeMsg.class));
        ArgumentCaptor<ProcessEvent> eventArg = ArgumentCaptor.forClass(ProcessEvent.class);
        verify(processEventRepo).saveAndFlush(eventArg.capture());
        assertThat(eventArg.getValue().getContainer()).isEqualTo(env.getContainer());
        assertThat(eventArg.getValue().getZipFileName()).isEqualTo(env.getZipFileName());
        assertThat(eventArg.getValue().getEvent()).isEqualTo(DOC_PROCESSED_NOTIFICATION_SENT);
        ArgumentCaptor<Envelope> envArg = ArgumentCaptor.forClass(Envelope.class);
        verify(envelopeRepo).saveAndFlush(envArg.capture());
        assertThat(envArg.getValue().getContainer()).isEqualTo(env.getContainer());
        assertThat(envArg.getValue().getZipFileName()).isEqualTo(env.getZipFileName());
        assertThat(envArg.getValue().getStatus()).isEqualTo(NOTIFICATION_SENT);
        assertThat(successCount.get()).isEqualTo(1);
    }

    @Test
    void should_rethrow_exception_from_service_bus() {
        // given
        doThrow(InvalidMessageException.class)
            .when(serviceBusHelper)
            .sendMessage(any(EnvelopeMsg.class));
        Envelope env = envelope();

        // when
        assertThatThrownBy(() ->
                               orchestratorNotificationService.processEnvelope(successCount, env)
        )
            .isInstanceOf(InvalidMessageException.class);

        // then
        ArgumentCaptor<Envelope> envArg = ArgumentCaptor.forClass(Envelope.class);
        verify(envelopeRepo).saveAndFlush(envArg.capture());
        assertThat(envArg.getValue().getContainer()).isEqualTo(env.getContainer());
        assertThat(envArg.getValue().getZipFileName()).isEqualTo(env.getZipFileName());
        assertThat(envArg.getValue().getStatus()).isEqualTo(NOTIFICATION_SENT);
        ArgumentCaptor<ProcessEvent> argument = ArgumentCaptor.forClass(ProcessEvent.class);
        verify(processEventRepo).saveAndFlush(argument.capture());
        assertThat(argument.getValue().getContainer()).isEqualTo(env.getContainer());
        assertThat(argument.getValue().getZipFileName()).isEqualTo(env.getZipFileName());
        assertThat(argument.getValue().getEvent()).isEqualTo(DOC_PROCESSED_NOTIFICATION_SENT);
        assertThat(successCount.get()).isZero();
    }
}
