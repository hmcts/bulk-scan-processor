package uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg;

import uk.gov.hmcts.reform.bulkscanprocessor.entity.Classification;

import java.util.List;

public interface Msg {

    String getMsgId();

    byte[] getMsgBody();

    boolean isTestOnly();

    String getCaseNumber();

    Classification getClassification();

    String getJurisdiction();

    List<String> getDocumentUrls();

}
