package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import static java.util.Collections.emptyList;

@Entity
@Table(name = "envelopes")
public class Envelope {

    @Id
    private UUID id;

    @JsonProperty("po_box")
    private String poBox;
    @JsonProperty("jurisdiction")
    private String jurisdiction;
    @JsonProperty("delivery_date")
    private Timestamp deliveryDate;
    @JsonProperty("opening_date")
    private Timestamp openingDate;
    @JsonProperty("zip_file_created_date")
    private Timestamp zipFileCreatedDate;
    @JsonProperty("zip_file_name")
    private String zipFileName;
    @JsonProperty("scannable_items")
    private List<ScannableItem> scannableItems;
    @JsonProperty("payments")
    private List<Payment> payments;
    @JsonProperty("non_scannable_items")
    private List<NonScannableItem> nonScannableItems;

    private Envelope() {
        // For use by hibernate.
    }

    public Envelope(
        @JsonProperty("po_box") String poBox,
        @JsonProperty("jurisdiction") String jurisdiction,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm:ss.SSSSSS")
        @JsonProperty("delivery_date") Timestamp deliveryDate,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm:ss.SSSSSS")
        @JsonProperty("opening_date") Timestamp openingDate,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm:ss.SSSSSS")
        @JsonProperty("zip_file_createddate") Timestamp zipFileCreatedDate,
        @JsonProperty("zip_file_name") String zipFileName,
        @JsonProperty("scannable_items") List<ScannableItem> scannableItems,
        @JsonProperty("payments") List<Payment> payments,
        @JsonProperty("non_scannable_items") List<NonScannableItem> nonScannableItems
    ) {
        this.poBox = poBox;
        this.jurisdiction = jurisdiction;
        this.deliveryDate = deliveryDate;
        this.openingDate = openingDate;
        this.zipFileCreatedDate = zipFileCreatedDate;
        this.zipFileName = zipFileName;
        this.scannableItems = scannableItems == null ? emptyList() : scannableItems;
        this.payments = payments == null ? emptyList() : payments;
        this.nonScannableItems = nonScannableItems == null ? emptyList() : nonScannableItems;
    }

    public List<ScannableItem> getScannableItems() {
        return scannableItems;
    }

    public List<NonScannableItem> getNonScannableItems() {
        return nonScannableItems;
    }

    public List<Payment> getPayments() {
        return payments;
    }
}
