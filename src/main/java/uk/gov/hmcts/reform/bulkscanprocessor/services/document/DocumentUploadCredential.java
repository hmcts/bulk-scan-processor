package uk.gov.hmcts.reform.bulkscanprocessor.services.document;

public class DocumentUploadCredential {

    public final String s2sToken;
    public final String idamAccessToken;
    public final String caseTypeId;

    public DocumentUploadCredential(String s2sToken, String idamAccessToken, String caseTypeId) {
        this.s2sToken = s2sToken;
        this.idamAccessToken = idamAccessToken;
        this.caseTypeId = caseTypeId;
    }
}

