package uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Classification;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItem;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

public class EnvelopeMsg implements Msg {

    @JsonProperty("id")
    private final String envelopeId;

    @JsonProperty("case_ref")
    private String caseNumber;

    @JsonProperty("jurisdiction")
    private String jurisdiction;

    @JsonProperty("classification")
    private Classification classification;

    @JsonProperty("delivery_date")
    private LocalDateTime deliveryDate;

    @JsonProperty("opening_date")
    private LocalDateTime openingDate;

    @JsonProperty("zip_file_name")
    private String zipFileName;

    // TODO: remove, after no longer used by orchestrator
    @JsonProperty("doc_urls")
    private List<String> documentUrls;

    @JsonProperty("documents")
    private List<Document> documents;

    private final boolean testOnly;

    public EnvelopeMsg(Envelope envelope) {
        this.envelopeId = isNull(envelope.getId()) ? null : envelope.getId().toString();
        this.caseNumber = envelope.getCaseNumber();
        this.classification = envelope.getClassification();
        this.jurisdiction = envelope.getJurisdiction();
        this.deliveryDate = envelope.getDeliveryDate().toLocalDateTime();
        this.openingDate = envelope.getOpeningDate().toLocalDateTime();
        this.zipFileName = envelope.getZipFileName();
        this.testOnly = envelope.isTestOnly();
        this.documentUrls = envelope.getScannableItems()
            .stream()
            .map(ScannableItem::getDocumentUrl)
            .collect(Collectors.toList());
        this.documents = envelope
            .getScannableItems()
            .stream()
            .map(item -> new Document(
                item.getFileName(),
                item.getDocumentControlNumber(),
                item.getDocumentType(),
                item.getScanningDate().toInstant(),
                item.getDocumentUrl()
            )).collect(Collectors.toList());
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

    public String getJurisdiction() {
        return jurisdiction;
    }

    public List<String> getDocumentUrls() {
        return documentUrls;
    }

    public LocalDateTime getDeliveryDate() {
        return deliveryDate;
    }

    public LocalDateTime getOpeningDate() {
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
