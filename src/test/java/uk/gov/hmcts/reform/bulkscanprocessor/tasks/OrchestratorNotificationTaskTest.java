package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.InvalidMessageException;
import uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.ServiceBusHelper;

import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.EnvelopeCreator.envelope;

@ExtendWith(MockitoExtension.class)
public class OrchestratorNotificationTaskTest {

    @Mock private ServiceBusHelper serviceBusHelper;
    @Mock private EnvelopeRepository envelopeRepo;
    @Mock private ProcessEventRepository processEventRepo;

    private OrchestratorNotificationTask task;

    @BeforeEach
    public void setUp() throws Exception {
        this.task = new OrchestratorNotificationTask(
            serviceBusHelper,
            envelopeRepo,
            processEventRepo
        );
    }

    @Test
    public void should_try_to_send_all_envelopes_despite__previous_errors() {
        // given
        final int numberOfEnvelopesToSend = 5;

        given(
            envelopeRepo.findByStatus(Status.UPLOADED)
        ).willReturn(
            range(0, numberOfEnvelopesToSend).mapToObj(i -> envelope()).collect(toList())
        );

        doThrow(InvalidMessageException.class)
            .when(serviceBusHelper)
            .sendMessage(any());

        // when
        task.run();

        // then
        verify(serviceBusHelper, times(numberOfEnvelopesToSend))
            .sendMessage(any());
    }
}
