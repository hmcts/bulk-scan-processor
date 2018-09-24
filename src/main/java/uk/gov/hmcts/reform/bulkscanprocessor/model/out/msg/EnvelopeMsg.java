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

    @JsonProperty("zip_file_name")
    private String zipFileName;

    @JsonProperty("doc_urls")
    private List<String> documentUrls;

    private final boolean testOnly;

    public EnvelopeMsg(Envelope envelope) {
        this.envelopeId = isNull(envelope.getId()) ? null : envelope.getId().toString();
        this.caseNumber = envelope.getCaseNumber();
        this.classification = envelope.getClassification();
        this.jurisdiction = envelope.getJurisdiction();
        this.zipFileName = envelope.getZipFileName();
        this.testOnly = envelope.isTestOnly();
        this.documentUrls = envelope.getScannableItems()
            .stream()
            .map(ScannableItem::getDocumentUrl)
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

    public String getJurisdiction() {
        return jurisdiction;
    }

    public List<String> getDocumentUrls() {
        return documentUrls;
    }

    public String getZipFileName() {
        return zipFileName;
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
