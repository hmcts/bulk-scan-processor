package uk.gov.hmcts.reform.bulkscanprocessor.model.out.zipfilestatus;

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.NonScannableItemResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.PaymentResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.ScannableItemResponse;

import java.util.List;

public class ZipFileEnvelope {

    @JsonProperty("id")
    public final String id;

    @JsonProperty("container")
    public final String container;

    @JsonProperty("status")
    public final String status;

    @JsonProperty("ccd_id")
    public final String ccdId;

    @JsonProperty("envelope_ccd_action")
    public final String envelopeCcdAction;

    @JsonProperty("zip_deleted")
    public final boolean zipDeleted;

    @JsonProperty("rescan_for")
    public final String rescanFor;

    @JsonProperty("classification")
    public final Classification classification;

    @JsonProperty("jurisdiction")
    public final String jurisdiction;

    @JsonProperty("case_number")
    public final String caseNumber;

    @JsonProperty("scannable_items")
    public final List<ScannableItemResponse> scannableItems;

    @JsonProperty("non_scannable_items")
    public final List<NonScannableItemResponse> nonScannableItems;

    @JsonProperty("payments")
    public final List<PaymentResponse> payments;

    // region constructor
    public ZipFileEnvelope(
        String id,
        String container,
        String status,
        String ccdId,
        String envelopeCcdAction,
        boolean zipDeleted,
        String rescanFor,
        Classification classification,
        String jurisdiction,
        String caseNumber,

        List<ScannableItemResponse> scannableItems,
        List<NonScannableItemResponse> nonScannableItems,
        List<PaymentResponse> payments

    ) {
        this.id = id;
        this.container = container;
        this.status = status;
        this.ccdId = ccdId;
        this.envelopeCcdAction = envelopeCcdAction;
        this.zipDeleted = zipDeleted;
        this.rescanFor = rescanFor;
        this.classification = classification;
        this.jurisdiction = jurisdiction;
        this.caseNumber = caseNumber;
        this.scannableItems = scannableItems;
        this.nonScannableItems = nonScannableItems;
        this.payments = payments;
    }
    // endregion
}
