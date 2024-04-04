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

/**
 * Represents an envelope in the system.
 */
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
    /**
     * Default constructor for the Envelope.
     */
    public Envelope() {
        // For use by hibernate.
    }

    /**
     * Constructor for the Envelope.
     *
     * @param poBox The PO Box
     * @param jurisdiction The jurisdiction
     * @param deliveryDate The delivery date
     * @param openingDate The opening date
     * @param zipFileCreateddate The zip file created date
     * @param zipFileName The zip file name
     * @param caseNumber The case number
     * @param previousServiceCaseReference The previous service case reference
     * @param classification The classification
     * @param scannableItems The list of scannable items
     * @param payments The list of payments
     * @param nonScannableItems The list of non scannable items
     * @param container The container
     * @param rescanFor The rescan for
     */
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

    /**
     * Get the scannable items.
     */
    public List<ScannableItem> getScannableItems() {
        return scannableItems;
    }

    /**
     * Get the non scannable items.
     */
    public List<NonScannableItem> getNonScannableItems() {
        return nonScannableItems;
    }

    /**
     * Get the payments.
     */
    public List<Payment> getPayments() {
        return payments;
    }

    /**
     * Get the id.
     */
    public UUID getId() {
        return id;
    }

    /**
     * Get the container.
     */
    public String getContainer() {
        return container;
    }

    /**
     * Get the case number.
     */
    public String getCaseNumber() {
        return caseNumber;
    }

    /**
     * Get the previous service case reference.
     */
    public String getPreviousServiceCaseReference() {
        return previousServiceCaseReference;
    }

    /**
     * Set container.
     * @param container The container
     */
    public void setContainer(String container) {
        this.container = container;
    }

    /**
     * Get the jurisdiction.
     */
    public String getJurisdiction() {
        return jurisdiction;
    }

    /**
     * Get the zip file name.
     */
    public String getZipFileName() {
        return zipFileName;
    }

    /**
     * Set the zip file name.
     * @param zipFileName The zip file name
     */
    public void setZipFileName(String zipFileName) {
        this.zipFileName = zipFileName;
    }

    /**
     * Get the status.
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Set the status.
     * @param status The status
     */
    public void setStatus(Status status) {
        this.status = status;
    }

    /**
     * Get the created at.
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Get the PO Box.
     */
    public String getPoBox() {
        return poBox;
    }

    /**
     * Get the delivery date.
     */
    public Instant getDeliveryDate() {
        return deliveryDate;
    }

    /**
     * Get the opening date.
     */
    public Instant getOpeningDate() {
        return openingDate;
    }

    /**
     * Get the zip file created date.
     */
    public Instant getZipFileCreateddate() {
        return zipFileCreateddate;
    }

    /**
     * Get the classification.
     */
    public Classification getClassification() {
        return classification;
    }

    /**
     * Get the upload failure count.
     */
    public int getUploadFailureCount() {
        return uploadFailureCount;
    }

    /**
     * Set the upload failure count.
     * @param uploadFailureCount The upload failure count
     */
    public void setUploadFailureCount(int uploadFailureCount) {
        this.uploadFailureCount = uploadFailureCount;
    }

    /**
     * Check if zip deleted.
     */
    public boolean isZipDeleted() {
        return zipDeleted;
    }

    /**
     * Set zip deleted.
     * @param zipDeleted The zip deleted
     */
    public void setZipDeleted(boolean zipDeleted) {
        this.zipDeleted = zipDeleted;
    }

    /**
     * Get the rescan for.
     */
    public String getRescanFor() {
        return rescanFor;
    }

    /**
     * Set the rescan for.
     * @param rescanFor The rescan for
     */
    public void setRescanFor(String rescanFor) {
        this.rescanFor = rescanFor;
    }

    /**
     * Get the last modified.
     */
    public Instant getLastModified() {
        return lastModified;
    }

    /**
     * Check if the envelope is test only.
     */
    public boolean isTestOnly() {
        return !Strings.isNullOrEmpty(zipFileName)
            && zipFileName.toLowerCase(Locale.ENGLISH).endsWith(TEST_FILE_SUFFIX);
    }

    /**
     * Assign the envelope to the children.
     * @param assignables The list of scannable items
     */
    private void assignSelfToChildren(List<? extends EnvelopeAssignable> assignables) {
        assignables.forEach(assignable -> assignable.setEnvelope(this));
    }

    /**
     * Log if the container is missing.
     */
    @PrePersist
    public void containerMustBePresent() {
        if (container == null) {
            log.warn("Missing required container for {}", zipFileName);
        }
    }

    /**
     * Set the ccd id.
     * @param ccdId The ccd id
     */
    public void setCcdId(String ccdId) {
        this.ccdId = ccdId;
    }

    /**
     * Set the envelope ccd action.
     * @param envelopeCcdAction The envelope ccd action
     */
    public void setEnvelopeCcdAction(String envelopeCcdAction) {
        this.envelopeCcdAction = envelopeCcdAction;
    }

    /**
     * Get the ccd id.
     */
    public String getCcdId() {
        return ccdId;
    }

    /**
     * Get the envelope ccd action.
     */
    public String getEnvelopeCcdAction() {
        return envelopeCcdAction;
    }
}
