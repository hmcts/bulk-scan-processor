package uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models;

import java.util.List;

public class ReceivedZipFileData {
    public final ZipFileIdentifier zipFileIdentifier;
    public final List<String> scannableItemDcns;
    public final List<String> paymentDcns;

    public ReceivedZipFileData(
        ZipFileIdentifier zipFileIdentifier,
        List<String> scannableItemDcns,
        List<String> paymentDcns
    ) {
        this.zipFileIdentifier = zipFileIdentifier;
        this.scannableItemDcns = scannableItemDcns;
        this.paymentDcns = paymentDcns;
    }
}
