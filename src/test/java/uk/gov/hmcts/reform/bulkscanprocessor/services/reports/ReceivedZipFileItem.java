package uk.gov.hmcts.reform.bulkscanprocessor.services.reports;

import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.ReceivedZipFile;

import java.time.Instant;
import java.util.UUID;

public class ReceivedZipFileItem implements ReceivedZipFile {
    private final String zipFileName;
    private final String container;
    private final Instant processingStartedEventDate;
    private final String scannableItemDcn;
    private final String paymentDcn;
    private final String rescanFor;
    private final UUID envelopeId;

    public ReceivedZipFileItem(
        String zipFileName,
        String container,
        Instant processingStartedEventDate,
        String scannableItemDcn,
        String paymentDcn,
        String rescanFor,
        UUID envelopeId
    ) {
        this.zipFileName = zipFileName;
        this.container = container;
        this.processingStartedEventDate = processingStartedEventDate;
        this.scannableItemDcn = scannableItemDcn;
        this.paymentDcn = paymentDcn;
        this.rescanFor = rescanFor;
        this.envelopeId = envelopeId;
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

    @Override
    public String getRescanFor() {
        return rescanFor;
    }

    @Override
    public UUID getEnvelopeId() {
        return envelopeId;
    }
}
