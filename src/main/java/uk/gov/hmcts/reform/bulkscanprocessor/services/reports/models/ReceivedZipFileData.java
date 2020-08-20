package uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models;

import io.vavr.Tuple2;

import java.util.List;

public class ReceivedZipFileData {
    public final Tuple2 zipFileIdentifier;
    public final List<String> scannableItemDcns;
    public final List<String> paymentDcns;

    public ReceivedZipFileData(
        String zipFileName,
        String container,
        List<String> scannableItemDcns,
        List<String> paymentDcns
    ) {
        this.zipFileIdentifier = new Tuple2<>(zipFileName, container);
        this.scannableItemDcns = scannableItemDcns;
        this.paymentDcns = paymentDcns;
    }
}
