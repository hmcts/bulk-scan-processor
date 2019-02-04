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
        @JsonProperty("status") Status status,
        @JsonProperty("classification") String classification,
        @JsonProperty("scannable_items") List<ScannableItemResponse> scannableItems,
        @JsonProperty("payments") List<PaymentResponse> payments,
        @JsonProperty("non_scannable_items") List<NonScannableItemResponse> nonScannableItems
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
        this.status = status;
        this.classification = classification;
        this.scannableItems = scannableItems == null ? emptyList() : scannableItems;
        this.payments = payments == null ? emptyList() : payments;
        this.nonScannableItems = nonScannableItems == null ? emptyList() : nonScannableItems;
    }

    public UUID getId() {
        return id;
    }

    public String getCaseNumber() {
        return caseNumber;
    }

    public Status getStatus() {
        return status;
    }

    public String getZipFileName() {
        return zipFileName;
    }

    public String getContainer() {
        return container;
    }

    public String getPoBox() {
        return poBox;
    }

    public String getJurisdiction() {
        return jurisdiction;
    }

    public Instant getDeliveryDate() {
        return deliveryDate;
    }

    public Instant getOpeningDate() {
        return openingDate;
    }

    public Instant getZipFileCreateddate() {
        return zipFileCreateddate;
    }

    public String getClassification() {
        return classification;
    }

    public List<ScannableItemResponse> getScannableItems() {
        return scannableItems;
    }

    public List<PaymentResponse> getPayments() {
        return payments;
    }

    public List<NonScannableItemResponse> getNonScannableItems() {
        return nonScannableItems;
    }

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
            + ", status=" + status
            + ", classification=" + classification
            + ", scannableItems=" + scannableItems
            + ", payments=" + payments
            + ", nonScannableItems=" + nonScannableItems
            + '}';
    }

}
