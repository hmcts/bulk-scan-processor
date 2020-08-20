package uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models;

import java.util.List;

public class ReceivedZipFileData {
    public final String zipFileName;
    public final String container;
    public final List<String> scannableItemDcns;
    public final List<String> paymentDcns;

    public ReceivedZipFileData(
        String zipFileName,
        String container,
        List<String> scannableItemDcns,
        List<String> paymentDcns
    ) {
        this.zipFileName = zipFileName;
        this.container = container;
        this.scannableItemDcns = scannableItemDcns;
        this.paymentDcns = paymentDcns;
    }
}
