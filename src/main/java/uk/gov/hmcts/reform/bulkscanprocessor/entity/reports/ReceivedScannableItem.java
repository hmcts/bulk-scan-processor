package uk.gov.hmcts.reform.bulkscanprocessor.entity.reports;

import java.time.Instant;

public interface ReceivedScannableItem {
    String getContainer();

    int getCount();
}
