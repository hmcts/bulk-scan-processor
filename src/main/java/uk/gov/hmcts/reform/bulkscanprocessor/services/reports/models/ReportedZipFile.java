package uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ReportedZipFile extends ZipFileData {
    public final String rescanFor;

    @JsonCreator
    public ReportedZipFile(
        @JsonProperty("zip_file_name") String zipFileName,
        @JsonProperty("container") String container,
        @JsonProperty("rescan_for") String rescanFor,
        @JsonProperty("scannable_item_dcns") List<String> scannableItemDcns,
        @JsonProperty("payment_dcns") List<String> paymentDcns
    ) {
        super(zipFileName, container, scannableItemDcns, paymentDcns);
        this.rescanFor = rescanFor;
    }
}
