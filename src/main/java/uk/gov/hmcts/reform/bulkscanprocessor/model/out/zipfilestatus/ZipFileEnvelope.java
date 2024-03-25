package uk.gov.hmcts.reform.bulkscanprocessor.model.out.zipfilestatus;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.NonScannableItemResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.PaymentResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.ScannableItemResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.util.InstantSerializer;

import java.time.Instant;
import java.util.List;

/**
 * Represents a zip file envelope.
 */
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

    @JsonProperty("zip_file_name")
    public final String zipFileName;

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

    @JsonProperty("created_at")
    @JsonSerialize(using = InstantSerializer.class)
    public final Instant createdAt;

    @JsonProperty("delivery_date")
    @JsonSerialize(using = InstantSerializer.class)
    public final Instant deliveryDate;

    @JsonProperty("opening_date")
    @JsonSerialize(using = InstantSerializer.class)
    public final Instant openingDate;

    @JsonProperty("scannable_items")
    public final List<ScannableItemResponse> scannableItems;

    @JsonProperty("non_scannable_items")
    public final List<NonScannableItemResponse> nonScannableItems;

    @JsonProperty("payments")
    public final List<PaymentResponse> payments;

    /**
     * Constructor for ZipFileEnvelope.
     * @param id envelope id
     * @param container container
     * @param status envelope status
     * @param ccdId ccd id
     * @param envelopeCcdAction envelope ccd action
     * @param zipFileName zip file name
     * @param zipDeleted whether the zip file is deleted
     * @param rescanFor rescan for
     * @param classification classification
     * @param jurisdiction jurisdiction
     * @param caseNumber case number
     * @param createdAt created at
     * @param deliveryDate delivery date
     * @param openingDate opening date
     * @param scannableItems list of scannable items
     * @param nonScannableItems list of non-scannable items
     * @param payments list of payments
     */
    public ZipFileEnvelope(
        String id,
        String container,
        String status,
        String ccdId,
        String envelopeCcdAction,
        String zipFileName,
        boolean zipDeleted,
        String rescanFor,
        Classification classification,
        String jurisdiction,
        String caseNumber,
        Instant createdAt,
        Instant deliveryDate,
        Instant openingDate,
        List<ScannableItemResponse> scannableItems,
        List<NonScannableItemResponse> nonScannableItems,
        List<PaymentResponse> payments
    ) {
        this.id = id;
        this.container = container;
        this.status = status;
        this.ccdId = ccdId;
        this.envelopeCcdAction = envelopeCcdAction;
        this.zipFileName = zipFileName;
        this.zipDeleted = zipDeleted;
        this.rescanFor = rescanFor;
        this.classification = classification;
        this.jurisdiction = jurisdiction;
        this.caseNumber = caseNumber;
        this.createdAt = createdAt;
        this.deliveryDate = deliveryDate;
        this.openingDate = openingDate;
        this.scannableItems = scannableItems;
        this.nonScannableItems = nonScannableItems;
        this.payments = payments;
    }
}
