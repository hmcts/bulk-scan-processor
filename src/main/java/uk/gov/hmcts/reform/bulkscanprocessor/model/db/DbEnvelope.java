package uk.gov.hmcts.reform.bulkscanprocessor.model.db;

import com.google.common.base.Strings;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Status;

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
public class DbEnvelope {

    private static final Logger log = LoggerFactory.getLogger(DbEnvelope.class);

    public static final String TEST_FILE_SUFFIX = ".test.zip";

    @Id
    @GeneratedValue
    private UUID id;

    private String caseNumber;

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

    private int uploadFailureCount = 0;

    private boolean zipDeleted = false;

    //We will need to retrieve all scannable item entities of Envelope every time hence fetch type is Eager
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "envelope")
    @Fetch(value = FetchMode.SUBSELECT)
    private List<DbScannableItem> scannableItems;

    //We will need to retrieve all payments entities of Envelope every time hence fetch type is Eager
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "envelope")
    @Fetch(value = FetchMode.SUBSELECT)
    private List<DbPayment> payments;

    //We will need to retrieve all non scannable item entities of Envelope every time hence fetch type is Eager
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "envelope")
    @Fetch(value = FetchMode.SUBSELECT)
    private List<DbNonScannableItem> nonScannableItems;

    // elevating to public access as javassist needs instantiation available when `this` proxy is passed to children
    public DbEnvelope() {
        // For use by hibernate.
    }

    public DbEnvelope(
        String poBox,
        String jurisdiction,
        Timestamp deliveryDate,
        Timestamp openingDate,
        Timestamp zipFileCreateddate,
        String zipFileName,
        String caseNumber,
        Classification classification,
        List<DbScannableItem> scannableItems,
        List<DbPayment> payments,
        List<DbNonScannableItem> nonScannableItems,
        String container
    ) {
        this.poBox = poBox;
        this.jurisdiction = jurisdiction;
        this.deliveryDate = deliveryDate;
        this.openingDate = openingDate;
        this.zipFileCreateddate = zipFileCreateddate;
        this.zipFileName = zipFileName;
        this.caseNumber = caseNumber;
        this.classification = classification;
        this.scannableItems = scannableItems == null ? emptyList() : scannableItems;
        this.payments = payments == null ? emptyList() : payments;
        this.nonScannableItems = nonScannableItems == null ? emptyList() : nonScannableItems;
        this.container = container;

        assignSelfToChildren(this.scannableItems);
        assignSelfToChildren(this.payments);
        assignSelfToChildren(this.nonScannableItems);
    }

    public List<DbScannableItem> getScannableItems() {
        return scannableItems;
    }

    public List<DbNonScannableItem> getNonScannableItems() {
        return nonScannableItems;
    }

    public List<DbPayment> getPayments() {
        return payments;
    }

    public UUID getId() {
        return id;
    }

    public String getContainer() {
        return container;
    }

    public String getCaseNumber() {
        return caseNumber;
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

    public int getUploadFailureCount() {
        return uploadFailureCount;
    }

    public void setUploadFailureCount(int uploadFailureCount) {
        this.uploadFailureCount = uploadFailureCount;
    }

    public boolean isZipDeleted() {
        return zipDeleted;
    }

    public void setZipDeleted(boolean zipDeleted) {
        this.zipDeleted = zipDeleted;
    }

    public boolean isTestOnly() {
        return !Strings.isNullOrEmpty(zipFileName)
            && zipFileName.toLowerCase().endsWith(TEST_FILE_SUFFIX);
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
