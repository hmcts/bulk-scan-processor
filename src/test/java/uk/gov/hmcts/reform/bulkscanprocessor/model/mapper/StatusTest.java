package uk.gov.hmcts.reform.bulkscanprocessor.model.mapper;

import org.junit.Test;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;

import static org.assertj.core.api.Assertions.assertThat;

public class StatusTest {

    @Test
    public void notification_sent_or_processed_only_is_processed() {
        for (Status s : Status.values()) {
            if (s == Status.NOTIFICATION_SENT || s == Status.PROCESSED || s == Status.COMPLETED) {
                assertThat(s.isProcessed()).as("check %s is processed == true", s).isTrue();
            } else {
                assertThat(s.isProcessed()).as("check %s is processed == false", s).isFalse();

            }
        }
    }

}
