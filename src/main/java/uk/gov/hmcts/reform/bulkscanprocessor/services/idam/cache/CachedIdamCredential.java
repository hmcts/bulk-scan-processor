package uk.gov.hmcts.reform.bulkscanprocessor.services.idam.cache;

public class CachedIdamCredential {

    public final String accessToken;
    public final String userId;
    public final long expiresIn;

    public CachedIdamCredential(String accessToken, String userId, long expiresIn) {
        this.accessToken = accessToken;
        this.userId = userId;
        this.expiresIn = expiresIn;
    }
}
