package uk.gov.hmcts.reform.bulkscanprocessor.services.document;

/**
 * Represents the credentials required to upload a document to CCD.
 */
public class DocumentUploadCredential {

    public final String s2sToken;
    public final String idamAccessToken;
    public final String caseTypeId;

    /**
     * Constructor for DocumentUploadCredential.
     * @param s2sToken The S2S token
     * @param idamAccessToken The IDAM access token
     * @param caseTypeId The case type ID
     */
    public DocumentUploadCredential(String s2sToken, String idamAccessToken, String caseTypeId) {
        this.s2sToken = s2sToken;
        this.idamAccessToken = idamAccessToken;
        this.caseTypeId = caseTypeId;
    }
}

