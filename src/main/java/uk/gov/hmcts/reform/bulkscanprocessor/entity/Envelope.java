package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.bulkscanprocessor.util.CustomTimestampDeserialiser;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.Table;

import static java.util.Collections.emptyList;

@Entity
@Table(name = "envelopes")
public class Envelope {

    private static final Logger log = LoggerFactory.getLogger(Envelope.class);

    @Id
    @GeneratedValue
    private UUID id;

    private String container;

    private String poBox;

    private String jurisdiction;

    private Timestamp deliveryDate;

    private Timestamp openingDate;

    private Timestamp zipFileCreateddate;

    private String zipFileName;

    @Enumerated(EnumType.STRING)
    private Status status = Status.CREATED;

    private Timestamp createdAt = Timestamp.from(Instant.now());

    @Enumerated(EnumType.STRING)
    private Classification classification = Classification.EXCEPTION;

    private boolean urgent = false;

    //We will need to retrieve all scannable item entities of Envelope every time hence fetch type is Eager
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "envelope")
    @Fetch(value = FetchMode.SUBSELECT)
    private List<ScannableItem> scannableItems;

    //We will need to retrieve all payments entities of Envelope every time hence fetch type is Eager
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "envelope")
    @Fetch(value = FetchMode.SUBSELECT)
    private List<Payment> payments;

    //We will need to retrieve all non scannable item entities of Envelope every time hence fetch type is Eager
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "envelope")
    @Fetch(value = FetchMode.SUBSELECT)
    private List<NonScannableItem> nonScannableItems;

    // elevating to public access as javassist needs instantiation available when `this` proxy is passed to children
    public Envelope() {
        // For use by hibernate.
    }

    @JsonCreator
    public Envelope(
        @JsonProperty("po_box") String poBox,
        @JsonProperty("jurisdiction") String jurisdiction,
        @JsonDeserialize(using = CustomTimestampDeserialiser.class)
        @JsonProperty("delivery_date") Timestamp deliveryDate,
        @JsonDeserialize(using = CustomTimestampDeserialiser.class)
        @JsonProperty("opening_date") Timestamp openingDate,
        @JsonDeserialize(using = CustomTimestampDeserialiser.class)
        @JsonProperty("zip_file_createddate") Timestamp zipFileCreateddate,
        @JsonProperty("zip_file_name") String zipFileName,
        @JsonProperty("classification") Classification classification,
        @JsonProperty("urgent") Boolean urgent,
        @JsonProperty("scannable_items") List<ScannableItem> scannableItems,
        @JsonProperty("payments") List<Payment> payments,
        @JsonProperty("non_scannable_items") List<NonScannableItem> nonScannableItems
    ) {
        this.poBox = poBox;
        this.jurisdiction = jurisdiction;
        this.deliveryDate = deliveryDate;
        this.openingDate = openingDate;
        this.zipFileCreateddate = zipFileCreateddate;
        this.zipFileName = zipFileName;
        this.classification = (classification == null ? Classification.EXCEPTION : classification);
        this.urgent = (urgent == null ? false : urgent);
        this.scannableItems = scannableItems == null ? emptyList() : scannableItems;
        this.payments = payments == null ? emptyList() : payments;
        this.nonScannableItems = nonScannableItems == null ? emptyList() : nonScannableItems;

        assignSelfToChildren(this.scannableItems);
        assignSelfToChildren(this.payments);
        assignSelfToChildren(this.nonScannableItems);
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

    public String getContainer() {
        return container;
    }

    public void setContainer(String container) {
        this.container = container;
    }

    public String getJurisdiction() {
        return jurisdiction;
    }

    public String getZipFileName() {
        return zipFileName;
    }

    public void setZipFileName(String zipFileName) {
        this.zipFileName = zipFileName;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public String getPoBox() {
        return poBox;
    }

    public Timestamp getDeliveryDate() {
        return deliveryDate;
    }

    public Timestamp getOpeningDate() {
        return openingDate;
    }

    public Timestamp getZipFileCreateddate() {
        return zipFileCreateddate;
    }

    public Classification getClassification() {
        return classification;
    }

    public boolean getUrgent() {
        return urgent;
    }

    private void assignSelfToChildren(List<? extends EnvelopeAssignable> assignables) {
        assignables.forEach(assignable -> assignable.setEnvelope(this));
    }

    @PrePersist
    public void containerMustBePresent() {
        if (container == null) {
            log.warn("Missing required container for {}", zipFileName);
        }
    }
    
}
