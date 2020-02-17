package uk.gov.hmcts.reform.bulkscanprocessor.services;

import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.StorageUri;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.SharedAccessBlobPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.config.AccessTokenProperties;
import uk.gov.hmcts.reform.bulkscanprocessor.config.AccessTokenProperties.TokenConfig;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ServiceConfigNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.UnableToGenerateSasTokenException;

import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.EnumSet;

import static com.microsoft.azure.storage.blob.SharedAccessBlobPermissions.LIST;
import static com.microsoft.azure.storage.blob.SharedAccessBlobPermissions.WRITE;
import static java.time.LocalDateTime.now;

@EnableConfigurationProperties(AccessTokenProperties.class)
@Service
public class SasTokenGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(SasTokenGeneratorService.class);

    private final CloudBlobClient cloudBlobClient;
    private final AccessTokenProperties accessTokenProperties;

    public SasTokenGeneratorService(
        CloudBlobClient cloudBlobClient,
        AccessTokenProperties accessTokenProperties
    ) {
        this.cloudBlobClient = cloudBlobClient;
        this.accessTokenProperties = accessTokenProperties;
    }

    public String generateSasToken(String serviceName) {
        StorageUri storageAccountUri = cloudBlobClient.getStorageUri();
        log.info("--> SAS Token request received for service {}. Account URI: {}", serviceName, storageAccountUri);

        try {
            return cloudBlobClient
                .getContainerReference(serviceName)
                .generateSharedAccessSignature(createSharedAccessPolicy(serviceName), null);

        } catch (URISyntaxException | StorageException | InvalidKeyException e) {
            throw new UnableToGenerateSasTokenException(e);
        }
    }

    private SharedAccessBlobPolicy createSharedAccessPolicy(String serviceName) {
        TokenConfig config = getTokenConfigForService(serviceName);

        SharedAccessBlobPolicy policy = new SharedAccessBlobPolicy();
        policy.setPermissions(EnumSet.of(WRITE, LIST));
        policy.setSharedAccessExpiryTime(Date.from(now().plusSeconds(config.getValidity()).toInstant(ZoneOffset.UTC)));

        return policy;
    }

    private TokenConfig getTokenConfigForService(String serviceName) {
        return accessTokenProperties.getServiceConfig().stream()
            .filter(tokenConfig -> tokenConfig.getServiceName().equalsIgnoreCase(serviceName))
            .findFirst()
            .orElseThrow(
                () -> new ServiceConfigNotFoundException("No service configuration found for service " + serviceName)
            );
    }

}
