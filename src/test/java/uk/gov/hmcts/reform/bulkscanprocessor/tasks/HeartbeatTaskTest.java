package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.ServiceBusHelper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class HeartbeatTaskTest {

    @Test
    public void should_send_heartbeat_message_to_the_queue() {
        // given
        ServiceBusHelper serviceBusHelper = Mockito.mock(ServiceBusHelper.class);
        HeartbeatTask task = new HeartbeatTask(serviceBusHelper);

        // when
        task.run();

        // then
        verify(serviceBusHelper).sendMessage(any());
    }
}
