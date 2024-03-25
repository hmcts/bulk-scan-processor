package uk.gov.hmcts.reform.bulkscanprocessor.services.idam.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.services.idam.Credential;
import uk.gov.hmcts.reform.bulkscanprocessor.services.idam.JurisdictionToUserMapping;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.TokenResponse;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

/**
 * A client that caches IDAM credentials.
 */
@Service
public class IdamCachedClient {

    private static final Logger log = LoggerFactory.getLogger(IdamCachedClient.class);
    public static final String BEARER_AUTH_TYPE = "Bearer ";
    public static final String EXPIRES_IN = "expires_in";

    private Cache<String, CachedIdamCredential> idamCache;

    private final IdamClient idamClient;
    private final JurisdictionToUserMapping users;

    /**
     * Constructor for IdamCachedClient.
     * @param idamClient The IDAM client
     * @param users The users
     * @param idamCacheExpiry The IDAM cache expiry
     */
    public IdamCachedClient(
        IdamClient idamClient,
        JurisdictionToUserMapping users,
        IdamCacheExpiry idamCacheExpiry
    ) {
        this.idamClient = idamClient;
        this.users = users;
        this.idamCache = Caffeine.newBuilder()
            .expireAfter(idamCacheExpiry)
            .build();
    }

    /**
     * Gets the IDAM credentials for the given jurisdiction.
     * @param jurisdiction The jurisdiction
     * @return The IDAM credentials
     */
    public CachedIdamCredential getIdamCredentials(String jurisdiction) {
        log.info("Getting idam credential for jurisdiction: {} ", jurisdiction);
        return this.idamCache.get(jurisdiction.toLowerCase(), this::retrieveIdamInfo);
    }

    /**
     * Removes the access token from the cache for the given jurisdiction.
     * @param jurisdiction The jurisdiction
     */
    public void removeAccessTokenFromCache(String jurisdiction) {
        log.info("Removing idam credential from cache for jurisdiction: {} ", jurisdiction);
        this.idamCache.invalidate(jurisdiction.toLowerCase());
    }

    /**
     * Retrieves the IDAM credentials for the given jurisdiction.
     * @param jurisdiction The jurisdiction
     * @return The IDAM credentials
     */
    private CachedIdamCredential retrieveIdamInfo(String jurisdiction) {
        log.info("Retrieving access token for jurisdiction: {} from IDAM", jurisdiction);
        Credential user = users.getUser(jurisdiction);
        TokenResponse tokenResponse = idamClient
            .getAccessTokenResponse(
                user.getUsername(),
                user.getPassword()
            );

        log.info(
            "Retrieving user details for jurisdiction: {} from IDAM, token scope: {}",
            jurisdiction,
            tokenResponse.scope
        );

        String tokenWithBearer = BEARER_AUTH_TYPE + tokenResponse.accessToken;
        UserInfo userDetails = idamClient.getUserInfo(tokenWithBearer);
        return new CachedIdamCredential(tokenWithBearer, userDetails.getUid(), Long.valueOf(tokenResponse.expiresIn));
    }

}
