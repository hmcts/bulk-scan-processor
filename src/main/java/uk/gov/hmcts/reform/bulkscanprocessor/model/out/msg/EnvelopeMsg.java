package uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Classification;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItem;

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

    @JsonProperty("doc_urls")
    private List<String> documentUrls;

    @JsonIgnore
    private final byte[] envelope;

    @JsonIgnore
    private final boolean testOnly;

    public EnvelopeMsg(Envelope envelope, boolean testOnly) {
        this.envelopeId = isNull(envelope.getId()) ? null : envelope.getId().toString();
        this.envelope = new byte[0];
        this.caseNumber = envelope.getCaseNumber();
        this.classification = envelope.getClassification();
        this.jurisdiction = envelope.getJurisdiction();
        this.documentUrls = envelope.getScannableItems()
            .stream()
            .map(ScannableItem::getDocumentUrl)
            .collect(Collectors.toList());
        this.testOnly = testOnly;
    }

    @Override
    @JsonIgnore
    public String getMsgId() {
        return envelopeId;
    }

    @Override
    @JsonIgnore
    public byte[] getMsgBody() {
        return envelope;
    }

    @Override
    public boolean isTestOnly() {
        return testOnly;
    }

    @Override
    public String getCaseNumber() {
        return caseNumber;
    }

    @Override
    public Classification getClassification() {
        return classification;
    }

    @Override
    public String getJurisdiction() {
        return jurisdiction;
    }

    @Override
    public List<String> getDocumentUrls() {
        return documentUrls;
    }

    @Override
    public String toString() {
        return "EnvelopeMsg{"
            + "envelopeId='" + envelopeId + "'"
            + "caseNumber='" + caseNumber + "'"
            + "classification='" + classification + "'"
            + "jurisdiction='" + jurisdiction + "'"
            + "documentUrls=[" + String.join(",", documentUrls) + "]"
            + "testOnly='" + testOnly + "'"
            + "}";
    }

}
