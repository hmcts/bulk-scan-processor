package uk.gov.hmcts.reform.bulkscanprocessor.services.reports;

import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.ReceivedPayment;

public class ReceivedPaymentItem implements ReceivedPayment {
    private final String container;
    private final int count;

    public ReceivedPaymentItem(
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
