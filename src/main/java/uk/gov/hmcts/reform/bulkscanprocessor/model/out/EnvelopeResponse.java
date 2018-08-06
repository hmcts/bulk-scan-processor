package uk.gov.hmcts.reform.bulkscanprocessor.model.out;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.util.CustomTimestampDeserialiser;
import uk.gov.hmcts.reform.bulkscanprocessor.util.CustomTimestampSerialiser;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import static java.util.Collections.emptyList;

public class EnvelopeResponse {

    @JsonIgnore
    private UUID id;

    @JsonProperty("container")
    private String container;

    @JsonProperty("po_box")
    private String poBox;

    @JsonProperty("jurisdiction")
    private String jurisdiction;

    @JsonSerialize(using = CustomTimestampSerialiser.class)
    @JsonProperty("delivery_date")
    private Timestamp deliveryDate;

    @JsonSerialize(using = CustomTimestampSerialiser.class)
    @JsonProperty("opening_date")
    private Timestamp openingDate;

    @JsonSerialize(using = CustomTimestampSerialiser.class)
    @JsonProperty("zip_file_createddate")
    private Timestamp zipFileCreateddate;

    @JsonProperty("zip_file_name")
    private String zipFileName;

    @JsonProperty("status")
    private Status status = Status.CREATED;

    @JsonProperty("scannable_items")
    private List<ScannableItemResponse> scannableItems;

    @JsonProperty("payments")
    private List<PaymentResponse> payments;

    @JsonProperty("non_scannable_items")
    private List<NonScannableItemResponse> nonScannableItems;

    @JsonCreator
    public EnvelopeResponse(
        @JsonProperty("po_box") String poBox,
        @JsonProperty("jurisdiction") String jurisdiction,
        @JsonDeserialize(using = CustomTimestampDeserialiser.class)
        @JsonProperty("delivery_date") Timestamp deliveryDate,
        @JsonDeserialize(using = CustomTimestampDeserialiser.class)
        @JsonProperty("opening_date") Timestamp openingDate,
        @JsonDeserialize(using = CustomTimestampDeserialiser.class)
        @JsonProperty("zip_file_createddate") Timestamp zipFileCreateddate,
        @JsonProperty("zip_file_name") String zipFileName,
        @JsonProperty("scannable_items") List<ScannableItemResponse> scannableItems,
        @JsonProperty("payments") List<PaymentResponse> payments,
        @JsonProperty("non_scannable_items") List<NonScannableItemResponse> nonScannableItems
    ) {
        this.poBox = poBox;
        this.jurisdiction = jurisdiction;
        this.deliveryDate = deliveryDate;
        this.openingDate = openingDate;
        this.zipFileCreateddate = zipFileCreateddate;
        this.zipFileName = zipFileName;
        this.scannableItems = scannableItems == null ? emptyList() : scannableItems;
        this.payments = payments == null ? emptyList() : payments;
        this.nonScannableItems = nonScannableItems == null ? emptyList() : nonScannableItems;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setContainer(String container) {
        this.container = container;
    }

    public String getZipFileName() {
        return zipFileName;
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
            + ", container='" + container + '\''
            + ", poBox='" + poBox + '\''
            + ", jurisdiction='" + jurisdiction + '\''
            + ", deliveryDate=" + deliveryDate
            + ", openingDate=" + openingDate
            + ", zipFileCreateddate=" + zipFileCreateddate
            + ", zipFileName='" + zipFileName + '\''
            + ", status=" + status
            + ", scannableItems=" + scannableItems
            + ", payments=" + payments
            + ", nonScannableItems=" + nonScannableItems
            + '}';
    }

}
