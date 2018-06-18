package uk.gov.hmcts.reform.bulkscanning.services;

import com.microsoft.azure.storage.StorageCredentials;
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
import uk.gov.hmcts.reform.bulkscanning.config.AccessTokenConfiguration;
import uk.gov.hmcts.reform.bulkscanning.exceptions.ServiceConfigNotFoundException;
import uk.gov.hmcts.reform.bulkscanning.exceptions.UnableToGenerateSasTokenException;

import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

@EnableConfigurationProperties(AccessTokenConfiguration.class)
@Service
public class SasTokenGeneratorService {
    private final CloudBlobClient cloudBlobClient;
    private final StorageCredentials storageCredentials;
    private final AccessTokenConfiguration accessTokenConfiguration;

    private static final Logger log = LoggerFactory.getLogger(SasTokenGeneratorService.class);

    public SasTokenGeneratorService(
        CloudBlobClient cloudBlobClient,
        StorageCredentials storageCredentials,
        AccessTokenConfiguration accessTokenConfiguration
    ) {
        this.cloudBlobClient = cloudBlobClient;
        this.storageCredentials = storageCredentials;
        this.accessTokenConfiguration = accessTokenConfiguration;
    }

    public String generateSasToken(String serviceName) {
        //Based on the service name container reference would be returned for specific service
        try {
            log.info("SAS Token request received for service {} ", serviceName);

            CloudBlobContainer cloudBlobContainer = cloudBlobClient.getContainerReference(serviceName);

            return cloudBlobContainer.generateSharedAccessSignature(createSharedAccessPolicy(serviceName), null);
        } catch (URISyntaxException | StorageException | InvalidKeyException e) {
            throw new UnableToGenerateSasTokenException(e);
        }
    }

    private SharedAccessBlobPolicy createSharedAccessPolicy(String serviceName) {
        AccessTokenConfiguration.TokenConfig config = getTokenConfigForService(serviceName);

        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.setTime(new Date());
        cal.add(Calendar.SECOND, config.getValidity());

        SharedAccessBlobPolicy policy = new SharedAccessBlobPolicy();
        policy.setPermissions(getBlobPermissions(config));
        policy.setSharedAccessExpiryTime(cal.getTime());

        return policy;
    }

    private AccessTokenConfiguration.TokenConfig getTokenConfigForService(String serviceName) {
        return accessTokenConfiguration.getServiceConfig().stream()
            .filter(tokenConfig -> tokenConfig.getServiceName().equalsIgnoreCase(serviceName))
            .findFirst()
            .orElseThrow(
                () -> new ServiceConfigNotFoundException("No service configuration found for service " + serviceName)
            );
    }

    private EnumSet<SharedAccessBlobPermissions> getBlobPermissions(AccessTokenConfiguration.TokenConfig config) {
        EnumSet<SharedAccessBlobPermissions> blobPermissions = EnumSet.noneOf(SharedAccessBlobPermissions.class);

        List<String> configuredPermissions = Arrays.asList(config.getPermissions().split(","));

        configuredPermissions.forEach(perm -> {
            if (EnumUtils.isValidEnum(SharedAccessBlobPermissions.class, perm)) {
                blobPermissions.add(SharedAccessBlobPermissions.valueOf(perm));
            }
        });
        return blobPermissions;
    }
}
