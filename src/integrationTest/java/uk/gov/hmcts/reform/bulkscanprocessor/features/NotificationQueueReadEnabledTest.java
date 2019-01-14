package uk.gov.hmcts.reform.bulkscanprocessor.features;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
    properties = {
        "queues.read-notifications.enabled=true"
    }
)
@RunWith(SpringRunner.class)

public class NotificationQueueReadEnabledTest {

    @Autowired
    private ApplicationContext context;

    @Test
    public void should_have_error_notification_bean_for_queue_reading_loaded_in_context() {
        assertThat(context.containsBean("notifications-read")).isTrue();
    }
}
