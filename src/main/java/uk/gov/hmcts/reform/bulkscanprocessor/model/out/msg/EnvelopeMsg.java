package uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification;
import uk.gov.hmcts.reform.bulkscanprocessor.model.ocr.OcrData;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

public class EnvelopeMsg implements Msg {

    @JsonProperty("id")
    private final String envelopeId;

    @JsonProperty("case_ref")
    private final String caseNumber;

    @JsonProperty("po_box")
    private final String poBox;

    @JsonProperty("jurisdiction")
    private final String jurisdiction;

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
    private final OcrData ocrData;

    private final boolean testOnly;

    public EnvelopeMsg(Envelope envelope) {
        this.envelopeId = isNull(envelope.getId()) ? null : envelope.getId().toString();
        this.caseNumber = envelope.getCaseNumber();
        this.classification = envelope.getClassification();
        this.poBox = envelope.getPoBox();
        this.jurisdiction = envelope.getJurisdiction();
        this.deliveryDate = envelope.getDeliveryDate().toInstant();
        this.openingDate = envelope.getOpeningDate().toInstant();
        this.zipFileName = envelope.getZipFileName();
        this.testOnly = envelope.isTestOnly();
        this.documents = envelope
            .getScannableItems()
            .stream()
            .map(Document::fromScannableItem)
            .collect(Collectors.toList());

        this.ocrData = retrieveOcrData(envelope);
    }

    @Override
    @JsonIgnore
    public String getMsgId() {
        return envelopeId;
    }

    public String getCaseNumber() {
        return caseNumber;
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

    public OcrData getOcrData() {
        return ocrData;
    }

    @Override
    public boolean isTestOnly() {
        return testOnly;
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

    private OcrData retrieveOcrData(Envelope envelope) {
        return envelope
            .getScannableItems()
            .stream()
            .filter(si -> si.getOcrData() != null)
            .map(ScannableItem::getOcrData)
            .findFirst()
            .orElse(null);
    }
}
