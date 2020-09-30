package uk.gov.hmcts.reform.bulkscanprocessor.entity.reports;

import java.time.Instant;
import java.util.UUID;

public interface ReceivedZipFile {
    String getZipFileName();

    String getContainer();

    Instant getProcessingStartedEventDate();

    String getScannableItemDcn();

    String getPaymentDcn();

    String getRescanFor();

    UUID getEnvelopeId();

}
