package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import uk.gov.hmcts.reform.bulkscanprocessor.util.CustomTimestampDeserialiser;
import uk.gov.hmcts.reform.bulkscanprocessor.util.CustomTimestampSerialiser;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import static java.util.Collections.emptyList;

@Entity
@Table(name = "envelopes")
public class Envelope {

    @Id
    @GeneratedValue
    private UUID id;

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
    @JsonProperty("zip_file_created_date")
    private Timestamp zipFileCreatedDate;
    @JsonProperty("zip_file_name")
    private String zipFileName;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "envelope")
    @JsonProperty("scannable_items")
    private List<ScannableItem> scannableItems;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "envelope")
    @JsonProperty("payments")
    private List<Payment> payments;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "envelope")
    @JsonProperty("non_scannable_items")
    private List<NonScannableItem> nonScannableItems;

    private Envelope() {
        // For use by hibernate.
    }

    public Envelope(
        @JsonProperty("po_box") String poBox,
        @JsonProperty("jurisdiction") String jurisdiction,
        @JsonDeserialize(using = CustomTimestampDeserialiser.class)
        @JsonProperty("delivery_date") Timestamp deliveryDate,
        @JsonDeserialize(using = CustomTimestampDeserialiser.class)
        @JsonProperty("opening_date") Timestamp openingDate,
        @JsonDeserialize(using = CustomTimestampDeserialiser.class)
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

    public UUID getId() {
        return id;
    }

    public String getJurisdiction() {
        return jurisdiction;
    }

    public String getZipFileName() {
        return zipFileName;
    }
}
