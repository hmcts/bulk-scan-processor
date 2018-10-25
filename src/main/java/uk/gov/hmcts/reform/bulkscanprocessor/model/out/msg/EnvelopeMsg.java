package uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Classification;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

public class EnvelopeMsg implements Msg {

    @JsonProperty("id")
    private final String envelopeId;

    @JsonProperty("case_ref")
    private String caseNumber;

    @JsonProperty("po_box")
    private String poBox;

    @JsonProperty("jurisdiction")
    private String jurisdiction;

    @JsonProperty("classification")
    private Classification classification;

    @JsonProperty("delivery_date")
    private Instant deliveryDate;

    @JsonProperty("opening_date")
    private Instant openingDate;

    @JsonProperty("zip_file_name")
    private String zipFileName;

    @JsonProperty("documents")
    private List<Document> documents;

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

    @Override
    public boolean isTestOnly() {
        return testOnly;
    }

    @Override
    @JsonIgnore
    public String toString() {
        return "EnvelopeMsg{"
            + "envelopeId='" + envelopeId + "'"
            + "testOnly='" + testOnly  + "'"
            + "zipFileName='" + zipFileName + "'"
            + "}";
    }

}
