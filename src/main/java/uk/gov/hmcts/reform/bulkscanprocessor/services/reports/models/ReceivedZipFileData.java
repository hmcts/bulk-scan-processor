package uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models;

import io.vavr.Tuple2;

import java.util.List;

public class ReceivedZipFileData {
    public final Tuple2 zipFileIdentifier;
    public final List<String> scannableItemDcns;
    public final List<String> paymentDcns;

    public ReceivedZipFileData(Tuple2 zipFileIdentifier, List<String> scannableItemDcns, List<String> paymentDcns) {
        this.zipFileIdentifier = zipFileIdentifier;
        this.scannableItemDcns = scannableItemDcns;
        this.paymentDcns = paymentDcns;
    }
}
