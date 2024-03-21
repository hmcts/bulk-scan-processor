package uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.DocumentType;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.OcrData;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.OcrDataField;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toList;

/**
 * Represents a message that is sent to the queue.
 */
public class EnvelopeMsg implements Msg {

    @JsonProperty("id")
    private final String envelopeId;

    @JsonProperty("case_ref")
    private final String caseNumber;

    @JsonProperty("previous_service_case_ref")
    private final String previousServiceCaseReference;

    @JsonProperty("po_box")
    private final String poBox;

    @JsonProperty("jurisdiction")
    private final String jurisdiction;

    @JsonProperty("container")
    private final String container;

    @JsonProperty("classification")
    private final Classification classification;

    @JsonProperty("delivery_date")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", timezone = "UTC")
    private final Instant deliveryDate;

    @JsonProperty("opening_date")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", timezone = "UTC")
    private final Instant openingDate;

    @JsonProperty("zip_file_name")
    private final String zipFileName;

    @JsonProperty("form_type")
    private final String formType;

    @JsonProperty("documents")
    private final List<Document> documents;

    @JsonProperty("payments")
    private final List<Payment> payments;

    @JsonProperty("ocr_data")
    private final List<OcrField> ocrData;

    @JsonProperty("ocr_data_validation_warnings")
    private final List<String> ocrDataValidationWarnings;

    private final boolean testOnly;

    /**
     * Constructor for EnvelopeMsg.
     * @param envelope envelope
     */
    public EnvelopeMsg(Envelope envelope) {
        this.envelopeId = isNull(envelope.getId()) ? null : envelope.getId().toString();
        this.caseNumber = envelope.getCaseNumber();
        this.previousServiceCaseReference = envelope.getPreviousServiceCaseReference();
        this.classification = envelope.getClassification();
        this.poBox = envelope.getPoBox();
        this.jurisdiction = envelope.getJurisdiction();
        this.container = envelope.getContainer();
        this.deliveryDate = envelope.getDeliveryDate();
        this.openingDate = envelope.getOpeningDate();
        this.zipFileName = envelope.getZipFileName();
        this.testOnly = envelope.isTestOnly();
        this.documents = convertFromDbScannableItems(envelope.getScannableItems());
        this.payments = convertFromDbPayments(envelope.getPayments());
        this.formType = findSubtypeOfScannableItemWithFormType(envelope);
        this.ocrData = retrieveOcrData(envelope);
        this.ocrDataValidationWarnings = retrieveOcrDataValidationWarnings(envelope);
    }

    /**
     * Returns the message ID.
     * @return message ID
     */
    @Override
    @JsonIgnore
    public String getMsgId() {
        return envelopeId;
    }

    /**
     * Returns the case number.
     * @return case number
     */
    public String getCaseNumber() {
        return caseNumber;
    }

    /**
     * Returns the previous service case reference.
     * @return previous service case reference
     */
    public String getPreviousServiceCaseReference() {
        return previousServiceCaseReference;
    }

    /**
     * Returns the classification.
     * @return classification
     */
    public Classification getClassification() {
        return classification;
    }

    /**
     * Returns the PO box.
     * @return PO box
     */
    public String getPoBox() {
        return poBox;
    }

    /**
     * Returns the jurisdiction.
     * @return jurisdiction
     */
    public String getJurisdiction() {
        return jurisdiction;
    }

    /**
     * Returns the delivery date.
     * @return delivery date
     */
    public Instant getDeliveryDate() {
        return deliveryDate;
    }

    /**
     * Returns the opening date.
     * @return opening date
     */
    public Instant getOpeningDate() {
        return openingDate;
    }

    /**
     * Returns the zip file name.
     * @return zip file name
     */
    public String getZipFileName() {
        return zipFileName;
    }

    /**
     * Returns the form type.
     * @return form type
     */
    public List<Document> getDocuments() {
        return documents;
    }

    /**
     * Get ocr data.
     * @return ocr data
     */
    public List<OcrField> getOcrData() {
        return ocrData;
    }

    /**
     * Get container.
     * @return container
     */
    public String getContainer() {
        return container;
    }

    /**
     * Get Label.
     * @return label
     */
    @Override
    public String getLabel() {
        return testOnly ? MsgLabel.TEST.toString() : null;
    }

    /**
     * String representation of the object.
     * @return string representation of the object
     */
    @Override
    @JsonIgnore
    public String toString() {
        return "EnvelopeMsg{"
            + "envelopeId='" + envelopeId + "'"
            + "testOnly='" + testOnly + "'"
            + "zipFileName='" + zipFileName + "'"
            + "}";
    }

    /**
     * Get ocr data validation warnings.
     * @param envelope envelope
     * @return ocr data validation warnings
     */
    private List<String> retrieveOcrDataValidationWarnings(Envelope envelope) {
        return findScannableItemsWithOcrData(envelope)
            .map(item ->
                item.getOcrValidationWarnings() != null
                    ? asList(item.getOcrValidationWarnings())
                    : Collections.<String>emptyList()
            )
            .findFirst()
            .orElse(emptyList());
    }

    /**
     * Get ocr data.
     * @param envelope envelope
     * @return ocr data
     */
    private List<OcrField> retrieveOcrData(Envelope envelope) {
        return findScannableItemsWithOcrData(envelope)
            .map(item -> convertFromInputOcrData(item.getOcrData()))
            .findFirst()
            .orElse(null);
    }

    /**
     * Find scannable items with ocr data.
     * @param envelope envelope
     * @return scannable items with ocr data
     */
    private Stream<ScannableItem> findScannableItemsWithOcrData(Envelope envelope) {
        return envelope
            .getScannableItems()
            .stream()
            .filter(si -> si.getOcrData() != null);
    }

    /**
     * Find subtype of scannable item with form type.
     * @param envelope envelope
     * @return subtype of scannable item with form type
     */
    private String findSubtypeOfScannableItemWithFormType(Envelope envelope) {
        return envelope
            .getScannableItems()
            .stream()
            .filter(si -> DocumentType.FORM.equals(si.getDocumentType()))
            .findFirst()
            .map(ScannableItem::getDocumentSubtype)
            .orElse(null);
    }

    /**
     * Convert from input ocr data.
     * @param inputOcrData input ocr data
     * @return ocr fields
     */
    private List<OcrField> convertFromInputOcrData(OcrData inputOcrData) {
        return inputOcrData
            .fields
            .stream()
            .map(this::convertFromInputOcrDataField)
            .collect(toList());
    }

    /**
     * Convert from input ocr data field.
     * @param inputField input field
     * @return ocr field
     */
    private OcrField convertFromInputOcrDataField(OcrDataField inputField) {
        String value = inputField.value != null
            ? inputField.value.asText("")
            : "";

        return new OcrField(inputField.name.textValue(), value);
    }

    /**
     * Convert from db scannable items.
     * @param dbScannableItems db scannable items
     * @return documents
     */
    private List<Document> convertFromDbScannableItems(List<ScannableItem> dbScannableItems) {
        return dbScannableItems
            .stream()
            .map(Document::fromScannableItem)
            .collect(toList());
    }

    /**
     * Convert from db payments.
     * @param dbPayments db payments
     * @return payments
     */
    private List<Payment> convertFromDbPayments(
        List<uk.gov.hmcts.reform.bulkscanprocessor.entity.Payment> dbPayments
    ) {
        return dbPayments
            .stream()
            .map(p -> new Payment(p.getDocumentControlNumber()))
            .collect(toList());
    }
}
