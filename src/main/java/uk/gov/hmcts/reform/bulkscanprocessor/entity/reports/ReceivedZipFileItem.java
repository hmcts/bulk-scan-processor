package uk.gov.hmcts.reform.bulkscanprocessor.entity.reports;

import java.time.Instant;

public class ReceivedZipFileItem implements ReceivedZipFile {
    private final String zipFileName;
    private final String container;
    private final Instant processingStartedEventDate;
    private final String scannableItemDCN;
    private final String paymentDCN;

    public ReceivedZipFileItem(
        String zipFileName,
        String container,
        Instant processingStartedEventDate,
        String scannableItemDCN,
        String paymentDCN
    ) {
        this.zipFileName = zipFileName;
        this.container = container;
        this.processingStartedEventDate = processingStartedEventDate;
        this.scannableItemDCN = scannableItemDCN;
        this.paymentDCN = paymentDCN;
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
    public String getScannableItemDCN() {
        return scannableItemDCN;
    }

    @Override
    public String getPaymentDCN() {
        return paymentDCN;
    }
}
