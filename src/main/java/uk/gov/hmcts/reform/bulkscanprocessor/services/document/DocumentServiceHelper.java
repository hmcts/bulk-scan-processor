package uk.gov.hmcts.reform.bulkscanprocessor.services.document;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.bulkscanprocessor.services.idam.cache.CachedIdamCredential;
import uk.gov.hmcts.reform.bulkscanprocessor.services.idam.cache.IdamCachedClient;

import java.util.Locale;

/**
 * Helper class for document service.
 */
@Component
public class DocumentServiceHelper {

    public static final String CASE_TYPE = "_ExceptionRecord";

    private final AuthTokenGenerator authTokenGenerator;
    private final IdamCachedClient idamCachedClient;

    /**
     * Constructor for DocumentServiceHelper.
     * @param authTokenGenerator The auth token generator
     * @param idamCachedClient The IDAM cached client
     */
    public DocumentServiceHelper(
        AuthTokenGenerator authTokenGenerator,
        IdamCachedClient idamCachedClient
    ) {
        this.authTokenGenerator = authTokenGenerator;
        this.idamCachedClient = idamCachedClient;
    }

    /**
     * Creates a document upload credential.
     * @param jurisdiction The jurisdiction
     * @param container The container
     * @return The document upload credential
     */
    public DocumentUploadCredential createDocumentUploadCredential(
        String jurisdiction,
        String container
    ) {
        CachedIdamCredential idamCredentials = idamCachedClient.getIdamCredentials(jurisdiction);
        String s2sToken = authTokenGenerator.generate();
        String caseTypeId = container.toUpperCase(Locale.getDefault()) + CASE_TYPE;
        return new DocumentUploadCredential(s2sToken, idamCredentials.accessToken, caseTypeId);
    }
}
