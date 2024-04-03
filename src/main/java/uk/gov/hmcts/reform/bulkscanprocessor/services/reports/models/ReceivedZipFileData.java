package uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models;

import java.util.List;
import java.util.UUID;

/**
 * Represents the data for received zip file.
 */
public class ReceivedZipFileData {
    public final String zipFileName;
    public final String container;
    public final String rescanFor;
    public final List<String> scannableItemDcns;
    public final List<String> paymentDcns;
    public final UUID envelopeId;

    /**
     * Constructor for the ReceivedZipFileData.
     * @param zipFileName The zip file name
     * @param container The container
     * @param rescanFor The rescan for
     * @param scannableItemDcns The scannable item dcns
     * @param paymentDcns The payment dcns
     * @param envelopeId The envelope id
     */
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
