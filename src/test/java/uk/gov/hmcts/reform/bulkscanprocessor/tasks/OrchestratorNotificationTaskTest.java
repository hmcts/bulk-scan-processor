package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.services.OrchestratorNotificationService;

import java.util.concurrent.atomic.AtomicInteger;

import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.EnvelopeCreator.envelope;

@ExtendWith(MockitoExtension.class)
public class OrchestratorNotificationTaskTest {

    @Mock private OrchestratorNotificationService orchestratorNotificationService;
    @Mock private EnvelopeRepository envelopeRepo;

    private OrchestratorNotificationTask task;

    @BeforeEach
    public void setUp() throws Exception {
        this.task = new OrchestratorNotificationTask(
            orchestratorNotificationService,
            envelopeRepo
        );
    }

    @Test
    public void should_try_to_send_all_envelopes_despite_previous_errors() {
        // given
        final int numberOfEnvelopesToSend = 5;

        given(
            envelopeRepo.findByStatus(Status.UPLOADED)
        ).willReturn(
            range(0, numberOfEnvelopesToSend).mapToObj(i -> envelope()).collect(toList())
        );

        // when
        task.run();

        // then
        verify(orchestratorNotificationService, times(numberOfEnvelopesToSend))
            .processEnvelope(any(AtomicInteger.class), any(Envelope.class));
    }
}
