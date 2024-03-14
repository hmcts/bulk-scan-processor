package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import com.google.common.base.Strings;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@SuppressWarnings("PMD.TooManyFields") // entity class
@Entity
@Table(name = "envelopes")
public class Envelope {

    private static final Logger log = LoggerFactory.getLogger(Envelope.class);

    public static final String TEST_FILE_SUFFIX = ".test.zip";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String caseNumber;

    private String container;

    private String poBox;

    private String jurisdiction;

    private Instant deliveryDate;

    private Instant openingDate;

    private Instant zipFileCreateddate;

    private String zipFileName;

    @Enumerated(EnumType.STRING)
    private Status status = Status.CREATED;

    private final Instant createdAt = Instant.now();

    @Enumerated(EnumType.STRING)
    private Classification classification = Classification.EXCEPTION;

    private int uploadFailureCount = 0;

    private boolean zipDeleted = false;

    private String previousServiceCaseReference;

    private String ccdId;

    private String envelopeCcdAction;

    private String rescanFor;

    private Instant lastModified;

    //We will need to retrieve all scannable item entities of Envelope every time hence fetch type is Eager
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "envelope")
    @Fetch(FetchMode.SUBSELECT)
    private List<ScannableItem> scannableItems;

    //We will need to retrieve all payments entities of Envelope every time hence fetch type is Eager
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "envelope")
    @Fetch(FetchMode.SUBSELECT)
    private List<Payment> payments;

    //We will need to retrieve all non scannable item entities of Envelope every time hence fetch type is Eager
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "envelope")
    @Fetch(FetchMode.SUBSELECT)
    private List<NonScannableItem> nonScannableItems;

    // elevating to public access as javassist needs instantiation available when `this` proxy is passed to children
    public Envelope() {
        // For use by hibernate.
    }

    public Envelope(
        String poBox,
        String jurisdiction,
        Instant deliveryDate,
        Instant openingDate,
        Instant zipFileCreateddate,
        String zipFileName,
        String caseNumber,
        String previousServiceCaseReference,
        Classification classification,
        List<ScannableItem> scannableItems,
        List<Payment> payments,
        List<NonScannableItem> nonScannableItems,
        String container,
        String rescanFor
    ) {
        this.poBox = poBox;
        this.jurisdiction = jurisdiction;
        this.deliveryDate = deliveryDate;
        this.openingDate = openingDate;
        this.zipFileCreateddate = zipFileCreateddate;
        this.zipFileName = zipFileName;
        this.caseNumber = caseNumber;
        this.previousServiceCaseReference = previousServiceCaseReference;
        this.classification = classification;
        this.scannableItems = scannableItems;
        this.payments = payments;
        this.nonScannableItems = nonScannableItems;
        this.container = container;
        this.rescanFor = rescanFor;

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

    public String getCaseNumber() {
        return caseNumber;
    }

    public String getPreviousServiceCaseReference() {
        return previousServiceCaseReference;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getPoBox() {
        return poBox;
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

    public String getRescanFor() {
        return rescanFor;
    }

    public void setRescanFor(String rescanFor) {
        this.rescanFor = rescanFor;
    }

    public Instant getLastModified() {
        return lastModified;
    }

    public boolean isTestOnly() {
        return !Strings.isNullOrEmpty(zipFileName)
            && zipFileName.toLowerCase(Locale.ENGLISH).endsWith(TEST_FILE_SUFFIX);
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

    public void setCcdId(String ccdId) {
        this.ccdId = ccdId;
    }

    public void setEnvelopeCcdAction(String envelopeCcdAction) {
        this.envelopeCcdAction = envelopeCcdAction;
    }

    public String getCcdId() {
        return ccdId;
    }

    public String getEnvelopeCcdAction() {
        return envelopeCcdAction;
    }
}
