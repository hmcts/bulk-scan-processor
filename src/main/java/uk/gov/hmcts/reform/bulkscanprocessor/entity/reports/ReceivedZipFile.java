package uk.gov.hmcts.reform.bulkscanprocessor.entity.reports;

import java.time.Instant;

public interface ReceivedZipFile {
    String getZipFileName();

    String getContainer();

    Instant getProcessingStartedEventDate();

    String getScannableItemDcn();

    String getPaymentDcn();
}
