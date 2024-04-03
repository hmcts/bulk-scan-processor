package uk.gov.hmcts.reform.bulkscanprocessor.model.out;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.util.InstantDeserializer;
import uk.gov.hmcts.reform.bulkscanprocessor.util.InstantSerializer;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static java.util.Collections.emptyList;

/**
 * Represents the response containing an envelope.
 */
public class EnvelopeResponse {

    @JsonProperty("id")
    private final UUID id;

    @JsonProperty("case_number")
    private final String caseNumber;

    @JsonProperty("container")
    private final String container;

    @JsonProperty("po_box")
    private final String poBox;

    @JsonProperty("jurisdiction")
    private final String jurisdiction;

    @JsonSerialize(using = InstantSerializer.class)
    @JsonProperty("delivery_date")
    private final Instant deliveryDate;

    @JsonSerialize(using = InstantSerializer.class)
    @JsonProperty("opening_date")
    private final Instant openingDate;

    @JsonSerialize(using = InstantSerializer.class)
    @JsonProperty("zip_file_createddate")
    private final Instant zipFileCreateddate;

    @JsonProperty("zip_file_name")
    private final String zipFileName;

    @JsonProperty("rescan_for")
    private final String rescanFor;

    @JsonProperty("status")
    private final Status status;

    @JsonProperty("classification")
    private final String classification;

    @JsonProperty("scannable_items")
    private final List<ScannableItemResponse> scannableItems;

    @JsonProperty("payments")
    private final List<PaymentResponse> payments;

    @JsonProperty("non_scannable_items")
    private final List<NonScannableItemResponse> nonScannableItems;

    @JsonProperty("ccd_id")
    private final String ccdId;

    @JsonProperty("ccd_action")
    private final String envelopeCcdAction;

    /**
     * Constructor for EnvelopeResponse.
     * @param id The id
     * @param caseNumber The case number
     * @param container The container
     * @param poBox The PO box
     * @param jurisdiction The jurisdiction
     * @param deliveryDate The delivery date
     * @param openingDate The opening date
     * @param zipFileCreateddate The zip file created date
     * @param zipFileName The zip file name
     * @param rescanFor The rescan for
     * @param status The status
     * @param classification The classification
     * @param scannableItems List of scannable items
     * @param payments List of payments
     * @param nonScannableItems List of non scannable items
     * @param ccdId The CCD ID
     * @param envelopeCcdAction The envelope CCD action
     */
    @JsonCreator
    public EnvelopeResponse(
        @JsonProperty("id") UUID id,
        @JsonProperty("case_number") String caseNumber,
        @JsonProperty("container") String container,
        @JsonProperty("po_box") String poBox,
        @JsonProperty("jurisdiction") String jurisdiction,
        @JsonDeserialize(using = InstantDeserializer.class)
        @JsonProperty("delivery_date") Instant deliveryDate,
        @JsonDeserialize(using = InstantDeserializer.class)
        @JsonProperty("opening_date") Instant openingDate,
        @JsonDeserialize(using = InstantDeserializer.class)
        @JsonProperty("zip_file_createddate") Instant zipFileCreateddate,
        @JsonProperty("zip_file_name") String zipFileName,
        @JsonProperty("rescan_for") String rescanFor,
        @JsonProperty("status") Status status,
        @JsonProperty("classification") String classification,
        @JsonProperty("scannable_items") List<ScannableItemResponse> scannableItems,
        @JsonProperty("payments") List<PaymentResponse> payments,
        @JsonProperty("non_scannable_items") List<NonScannableItemResponse> nonScannableItems,
        @JsonProperty("ccd_id") String ccdId,
        @JsonProperty("ccd_action") String envelopeCcdAction
    ) {
        this.id = id;
        this.caseNumber = caseNumber;
        this.container = container;
        this.poBox = poBox;
        this.jurisdiction = jurisdiction;
        this.deliveryDate = deliveryDate;
        this.openingDate = openingDate;
        this.zipFileCreateddate = zipFileCreateddate;
        this.zipFileName = zipFileName;
        this.rescanFor = rescanFor;
        this.status = status;
        this.classification = classification;
        this.scannableItems = scannableItems == null ? emptyList() : scannableItems;
        this.payments = payments == null ? emptyList() : payments;
        this.nonScannableItems = nonScannableItems == null ? emptyList() : nonScannableItems;
        this.ccdId = ccdId;
        this.envelopeCcdAction = envelopeCcdAction;
    }

    /**
     * Get the id.
     * @return The id
     */
    public UUID getId() {
        return id;
    }

    /**
     * Get the case number.
     * @return The case number
     */
    public String getCaseNumber() {
        return caseNumber;
    }

    /**
     * Get the status.
     * @return The status
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Get the zip file name.
     * @return The zip file name
     */
    public String getZipFileName() {
        return zipFileName;
    }

    /**
     * Get the container.
     * @return The container
     */
    public String getContainer() {
        return container;
    }

    /**
     * Get the PO box.
     * @return The PO box
     */
    public String getPoBox() {
        return poBox;
    }

    /**
     * Get the jurisdiction.
     * @return The jurisdiction
     */
    public String getJurisdiction() {
        return jurisdiction;
    }

    /**
     * Get the delivery date.
     * @return The delivery date
     */
    public Instant getDeliveryDate() {
        return deliveryDate;
    }

    /**
     * Get the opening date.
     * @return The opening date
     */
    public Instant getOpeningDate() {
        return openingDate;
    }

    /**
     * Get the zip file created date.
     * @return The zip file created date
     */
    public Instant getZipFileCreateddate() {
        return zipFileCreateddate;
    }

    /**
     * Get the classification.
     * @return The classification
     */
    public String getClassification() {
        return classification;
    }

    /**
     * Get the scannable items.
     * @return The scannable items
     */
    public List<ScannableItemResponse> getScannableItems() {
        return scannableItems;
    }

    /**
     * Get the payments.
     * @return The payments
     */
    public List<PaymentResponse> getPayments() {
        return payments;
    }

    /**
     * Get the non scannable items.
     * @return The non scannable items
     */
    public List<NonScannableItemResponse> getNonScannableItems() {
        return nonScannableItems;
    }

    /**
     * Get the CCD ID.
     * @return The CCD ID
     */
    public String getCcdId() {
        return ccdId;
    }

    /**
     * Get the envelope CCD action.
     * @return The envelope CCD action
     */
    public String getEnvelopeCcdAction() {
        return envelopeCcdAction;
    }

    /**
     * Get the rescan for.
     * @return The rescan for
     */
    public String getRescanFor() {
        return rescanFor;
    }

    /**
     * Get the envelope response as a string.
     * @return The envelope response as a string
     */
    @Override
    public String toString() {
        return "EnvelopeResponse{"
            + "id=" + id
            + ", caseNumber='" + caseNumber + '\''
            + ", container='" + container + '\''
            + ", poBox='" + poBox + '\''
            + ", jurisdiction='" + jurisdiction + '\''
            + ", deliveryDate=" + deliveryDate
            + ", openingDate=" + openingDate
            + ", zipFileCreateddate=" + zipFileCreateddate
            + ", zipFileName='" + zipFileName + '\''
            + ", rescanFor='" + rescanFor + '\''
            + ", status=" + status
            + ", classification=" + classification
            + ", scannableItems=" + scannableItems
            + ", payments=" + payments
            + ", nonScannableItems=" + nonScannableItems
            + ", ccdId=" + ccdId
            + ", envelopeCcdAction=" + envelopeCcdAction
            + '}';
    }
}
