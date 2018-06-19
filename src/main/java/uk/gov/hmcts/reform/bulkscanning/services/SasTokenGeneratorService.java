package uk.gov.hmcts.reform.bulkscanning.services;

import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.SharedAccessBlobPermissions;
import com.microsoft.azure.storage.blob.SharedAccessBlobPolicy;
import org.apache.commons.lang3.EnumUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanning.config.AccessTokenConfigurationProperties;
import uk.gov.hmcts.reform.bulkscanning.config.AccessTokenConfigurationProperties.TokenConfig;
import uk.gov.hmcts.reform.bulkscanning.exceptions.ServiceConfigNotFoundException;
import uk.gov.hmcts.reform.bulkscanning.exceptions.UnableToGenerateSasTokenException;

import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.TimeZone;

@EnableConfigurationProperties(AccessTokenConfigurationProperties.class)
@Service
public class SasTokenGeneratorService {
    private final CloudBlobClient cloudBlobClient;
    private final AccessTokenConfigurationProperties accessTokenConfigurationProperties;

    private static final Logger log = LoggerFactory.getLogger(SasTokenGeneratorService.class);

    public SasTokenGeneratorService(
        CloudBlobClient cloudBlobClient,
        AccessTokenConfigurationProperties accessTokenConfigurationProperties
    ) {
        this.cloudBlobClient = cloudBlobClient;
        this.accessTokenConfigurationProperties = accessTokenConfigurationProperties;
    }

    public String generateSasToken(String serviceName) {

        log.info("SAS Token request received for service {} ", serviceName);

        try {
            //Based on the service name container reference would be returned for specific service
            CloudBlobContainer cloudBlobContainer = cloudBlobClient.getContainerReference(serviceName);

            return cloudBlobContainer.generateSharedAccessSignature(createSharedAccessPolicy(serviceName), null);
        } catch (URISyntaxException | StorageException | InvalidKeyException e) {
            throw new UnableToGenerateSasTokenException(e);
        }
    }

    private SharedAccessBlobPolicy createSharedAccessPolicy(String serviceName) {
        TokenConfig config = getTokenConfigForService(serviceName);

        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.setTime(new Date());
        cal.add(Calendar.SECOND, config.getValidity());

        SharedAccessBlobPolicy policy = new SharedAccessBlobPolicy();
        policy.setPermissions(getBlobPermissions(config));
        policy.setSharedAccessExpiryTime(cal.getTime());

        return policy;
    }

    private TokenConfig getTokenConfigForService(String serviceName) {
        return accessTokenConfigurationProperties.getServiceConfig().stream()
            .filter(tokenConfig -> tokenConfig.getServiceName().equalsIgnoreCase(serviceName))
            .findFirst()
            .orElseThrow(
                () -> new ServiceConfigNotFoundException("No service configuration found for service " + serviceName)
            );
    }

    private EnumSet<SharedAccessBlobPermissions> getBlobPermissions(TokenConfig config) {
        EnumSet<SharedAccessBlobPermissions> blobPermissions = EnumSet.noneOf(SharedAccessBlobPermissions.class);

        for (String permission : config.getPermissions().split(",")) {
            if (EnumUtils.isValidEnum(SharedAccessBlobPermissions.class, permission)) {
                blobPermissions.add(SharedAccessBlobPermissions.valueOf(permission));
            }
        }

        return blobPermissions;
    }
}
