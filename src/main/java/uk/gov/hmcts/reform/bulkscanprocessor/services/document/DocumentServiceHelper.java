package uk.gov.hmcts.reform.bulkscanprocessor.services.document;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.bulkscanprocessor.services.idam.cache.CachedIdamCredential;
import uk.gov.hmcts.reform.bulkscanprocessor.services.idam.cache.IdamCachedClient;

import java.util.Locale;

@Component
public class DocumentServiceHelper {

    public static final String CASE_TYPE = "_ExceptionRecord";

    private final AuthTokenGenerator authTokenGenerator;
    private final IdamCachedClient idamCachedClient;


    public DocumentServiceHelper(
        AuthTokenGenerator authTokenGenerator,
        IdamCachedClient idamCachedClient
    ) {
        this.authTokenGenerator = authTokenGenerator;
        this.idamCachedClient = idamCachedClient;
    }

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
