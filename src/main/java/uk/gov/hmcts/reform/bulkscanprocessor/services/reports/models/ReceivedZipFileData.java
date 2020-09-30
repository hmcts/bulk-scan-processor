package uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models;

import java.util.List;
import java.util.UUID;

public class ReceivedZipFileData {
    public final String zipFileName;
    public final String container;
    public final String rescanFor;
    public final List<String> scannableItemDcns;
    public final List<String> paymentDcns;
    public final UUID envelopeId;

    public ReceivedZipFileData(
        String zipFileName,
        String container,
        String rescanFor,
        List<String> scannableItemDcns,
        List<String> paymentDcns,
        UUID envelopeId
    ) {
        this.zipFileName = zipFileName;
        this.container = container;
        this.rescanFor = rescanFor;
        this.scannableItemDcns = scannableItemDcns;
        this.paymentDcns = paymentDcns;
        this.envelopeId = envelopeId;
    }
}
