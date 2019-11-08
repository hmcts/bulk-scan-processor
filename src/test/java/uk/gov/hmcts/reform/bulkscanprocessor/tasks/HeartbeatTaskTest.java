package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.InvalidMessageException;
import uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.ServiceBusHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class HeartbeatTaskTest {

    @Mock private ServiceBusHelper serviceBusHelper;

    @Test
    public void should_send_heartbeat_message_to_the_queue() {
        // given
        HeartbeatTask task = new HeartbeatTask(serviceBusHelper);

        // when
        task.run();

        // then
        verify(serviceBusHelper).sendMessage(any());
    }

    @Test
    public void should_handle_exception_thrown_by_service_bus_client() {
        // given
        doThrow(new InvalidMessageException("msg"))
            .when(serviceBusHelper)
            .sendMessage(any());

        HeartbeatTask task = new HeartbeatTask(serviceBusHelper);

        // when
        Throwable exc = catchThrowable(() -> task.run());

        // then
        assertThat(exc).isNull();
    }
}
