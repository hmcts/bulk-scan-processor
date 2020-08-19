package uk.gov.hmcts.reform.bulkscanprocessor.entity.reports;

import java.time.Instant;

public class ReceivedZipFileItem implements ReceivedZipFile {
    private final String zipFileName;
    private final String container;
    private final Instant processingStartedEventDate;
    private final String scannableItemDcn;
    private final String paymentDcn;

    public ReceivedZipFileItem(
        String zipFileName,
        String container,
        Instant processingStartedEventDate,
        String scannableItemDcn,
        String paymentDcn
    ) {
        this.zipFileName = zipFileName;
        this.container = container;
        this.processingStartedEventDate = processingStartedEventDate;
        this.scannableItemDcn = scannableItemDcn;
        this.paymentDcn = paymentDcn;
    }

    @Override
    public String getZipFileName() {
        return zipFileName;
    }

    @Override
    public String getContainer() {
        return container;
    }

    @Override
    public Instant getProcessingStartedEventDate() {
        return processingStartedEventDate;
    }

    @Override
    public String getScannableItemDcn() {
        return scannableItemDcn;
    }

    @Override
    public String getPaymentDcn() {
        return paymentDcn;
    }
}
