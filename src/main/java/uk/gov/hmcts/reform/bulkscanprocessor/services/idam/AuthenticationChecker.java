package uk.gov.hmcts.reform.bulkscanprocessor.services.idam;

import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.NoUserConfiguredException;
import uk.gov.hmcts.reform.bulkscanprocessor.services.idam.cache.IdamCachedClient;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service to check if the user can sign in to all configured jurisdictions.
 */
@Service
@EnableConfigurationProperties(JurisdictionToUserMapping.class)
public class AuthenticationChecker {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationChecker.class);

    private final JurisdictionToUserMapping jurisdictionMapping;
    private final IdamCachedClient idamClient;

    /**
     * Constructor for AuthenticationChecker.
     * @param jurisdictionMapping The jurisdiction to user mapping
     * @param idamClient The IDAM client
     */
    public AuthenticationChecker(
        JurisdictionToUserMapping jurisdictionMapping,
        IdamCachedClient idamClient
    ) {
        this.jurisdictionMapping = jurisdictionMapping;
        this.idamClient = idamClient;
    }

    /**
     * Checks if the user can sign in to all configured jurisdictions.
     * @return The list of jurisdiction configuration statuses
     */
    public List<JurisdictionConfigurationStatus> checkSignInForAllJurisdictions() {
        return jurisdictionMapping.getUsers()
            .entrySet()
            .stream()
            .map(entry -> checkSignIn(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());
    }

    /**
     * Checks if the user can sign in to the given jurisdiction.
     * @param jurisdiction The jurisdiction
     * @return The jurisdiction configuration status
     */
    public JurisdictionConfigurationStatus checkSignInForJurisdiction(String jurisdiction) {
        try {
            return checkSignIn(jurisdiction, jurisdictionMapping.getUser(jurisdiction));
        } catch (NoUserConfiguredException exception) {
            return new JurisdictionConfigurationStatus(jurisdiction, false, exception.getMessage(), null);
        }
    }

    /**
     * Checks if the user can sign in to the given jurisdiction.
     * @param jurisdiction The jurisdiction
     * @param credential The credential
     * @return The jurisdiction configuration status
     */
    private JurisdictionConfigurationStatus checkSignIn(String jurisdiction, Credential credential) {
        try {
            idamClient.getIdamCredentials(jurisdiction);

            log.info("Successful authentication of {} jurisdiction", jurisdiction);

            return new JurisdictionConfigurationStatus(jurisdiction, true);
        } catch (FeignException e) {
            log.error(
                "An error occurred while authenticating {} jurisdiction with {} username",
                jurisdiction,
                credential.getUsername(),
                e
            );

            // temp fix until new version of idam-client is released
            String desc = e.responseBody().map(voidBody -> e.contentUTF8()).orElse(e.getMessage());
            return new JurisdictionConfigurationStatus(jurisdiction, false, desc, e.status());
        } catch (Exception e) {
            log.error(
                "An error occurred while authenticating {} jurisdiction with {} username",
                jurisdiction,
                credential.getUsername(),
                e
            );

            return new JurisdictionConfigurationStatus(jurisdiction, false, e.getMessage(), null);
        }
    }
}
