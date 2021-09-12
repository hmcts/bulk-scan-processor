package uk.gov.hmcts.reform.bulkscanprocessor.services.reports;

import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.ReceivedScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.RejectedZipFile;

import java.time.Instant;
import java.util.UUID;

public class ReceivedScannableItemItem implements ReceivedScannableItem {
    private final String container;
    private final int count;

    public ReceivedScannableItemItem(
            String container,
            int count
    ) {
        this.container = container;
        this.count = count;
    }

    @Override
    public String getContainer() {
        return container;
    }

    @Override
    public int getCount() {
        return count;
    }
}
