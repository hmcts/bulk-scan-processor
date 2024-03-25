package uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Represents the reported zip file.
 */
public class ReportedZipFile {
    public final String zipFileName;
    public final String container;
    public final String rescanFor;
    public final List<String> scannableItemDcns;
    public final List<String> paymentDcns;

    /**
     * Constructor for the ReportedZipFile.
     * @param zipFileName The zip file name
     * @param container The container
     * @param rescanFor The rescan for
     * @param scannableItemDcns The scannable item dcns
     * @param paymentDcns The payment dcns
     */
    @JsonCreator
    public ReportedZipFile(
        @JsonProperty("zip_file_name") String zipFileName,
        @JsonProperty("container") String container,
        @JsonProperty("rescan_for") String rescanFor,
        @JsonProperty("scannable_item_dcns") List<String> scannableItemDcns,
        @JsonProperty("payment_dcns") List<String> paymentDcns
    ) {
        this.zipFileName = zipFileName;
        this.container = container;
        this.rescanFor = rescanFor;
        this.scannableItemDcns = scannableItemDcns;
        this.paymentDcns = paymentDcns;
    }
}
