package uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models;

import java.util.List;

public class ReceivedZipFileData extends ZipFileData {
    public ReceivedZipFileData(
        String zipFileName,
        String container,
        List<String> scannableItemDcns,
        List<String> paymentDcns
    ) {
        super(zipFileName, container, scannableItemDcns, paymentDcns);
    }
}
