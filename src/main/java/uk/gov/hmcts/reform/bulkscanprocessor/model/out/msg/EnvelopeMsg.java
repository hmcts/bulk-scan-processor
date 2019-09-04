package uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification;
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
    private final Instant deliveryDate;

    @JsonProperty("opening_date")
    private final Instant openingDate;

    @JsonProperty("zip_file_name")
    private final String zipFileName;

    @JsonProperty("documents")
    private final List<Document> documents;

    @JsonProperty("ocr_data")
    private final List<OcrField> ocrData;

    @JsonProperty("ocr_data_validation_warnings")
    private final List<String> ocrDataValidationWarnings;

    private final boolean testOnly;

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
        this.documents = envelope
            .getScannableItems()
            .stream()
            .map(Document::fromScannableItem)
            .collect(toList());

        this.ocrData = retrieveOcrData(envelope);
        this.ocrDataValidationWarnings = retrieveOcrDataValidationWarnings(envelope);
    }

    // This method is here to allow for the field name change without downtime
    // TODO: remove when the orchestrator has switched to the new name - ocr_data_validation_warnings
    @JsonProperty("ocr_validation_warnings")
    public List<String> getOcrDataValidationWarnings() {
        return ocrDataValidationWarnings;
    }

    @Override
    @JsonIgnore
    public String getMsgId() {
        return envelopeId;
    }

    public String getCaseNumber() {
        return caseNumber;
    }

    public String getPreviousServiceCaseReference() {
        return previousServiceCaseReference;
    }

    public Classification getClassification() {
        return classification;
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

    public String getZipFileName() {
        return zipFileName;
    }

    public List<Document> getDocuments() {
        return documents;
    }

    public List<OcrField> getOcrData() {
        return ocrData;
    }

    public String getContainer() {
        return container;
    }

    @Override
    public String getLabel() {
        return testOnly ? MsgLabel.TEST.toString() : null;
    }

    @Override
    @JsonIgnore
    public String toString() {
        return "EnvelopeMsg{"
            + "envelopeId='" + envelopeId + "'"
            + "testOnly='" + testOnly + "'"
            + "zipFileName='" + zipFileName + "'"
            + "}";
    }

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

    private List<OcrField> retrieveOcrData(Envelope envelope) {
        return findScannableItemsWithOcrData(envelope)
            .map(item -> convertFromInputOcrData(item.getOcrData()))
            .findFirst()
            .orElse(null);
    }

    private Stream<ScannableItem> findScannableItemsWithOcrData(Envelope envelope) {
        return envelope
            .getScannableItems()
            .stream()
            .filter(si -> si.getOcrData() != null);
    }

    private List<OcrField> convertFromInputOcrData(OcrData inputOcrData) {
        return inputOcrData
            .fields
            .stream()
            .map(this::convertFromInputOcrDataField)
            .collect(toList());
    }

    private OcrField convertFromInputOcrDataField(OcrDataField inputField) {
        String value = inputField.value != null
            ? inputField.value.asText("")
            : "";

        return new OcrField(inputField.name.textValue(), value);
    }
}
