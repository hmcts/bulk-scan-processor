package uk.gov.hmcts.reform.bulkscanprocessor.services;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.sas.BlobContainerSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.config.AccessTokenProperties;
import uk.gov.hmcts.reform.bulkscanprocessor.config.AccessTokenProperties.TokenConfig;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ServiceConfigNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.UnableToGenerateSasTokenException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Service to generate SAS tokens for the services.
 */
@EnableConfigurationProperties(AccessTokenProperties.class)
@Service
public class SasTokenGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(SasTokenGeneratorService.class);

    private final BlobServiceClient blobServiceClient;
    private final AccessTokenProperties accessTokenProperties;
    private static final String PERMISSION_WRITE_LIST = "wlr";

    /**
     * Constructor for the SasTokenGeneratorService.
     * @param blobServiceClient The blob service client
     * @param accessTokenProperties The access token properties
     */
    public SasTokenGeneratorService(
        BlobServiceClient blobServiceClient,
        AccessTokenProperties accessTokenProperties
    ) {
        this.blobServiceClient = blobServiceClient;
        this.accessTokenProperties = accessTokenProperties;
    }

    /**
     * Generates SAS token for the given service.
     * @param serviceName The service name
     * @return The SAS token
     * @throws UnableToGenerateSasTokenException If unable to generate SAS token
     */
    public String generateSasToken(String serviceName) {
        String storageAccountUri = blobServiceClient.getAccountUrl();
        log.info("SAS Token request received for service {}. Account URI: {}", serviceName, storageAccountUri);
        TokenConfig config = getTokenConfigForService(serviceName);

        try {
            return blobServiceClient
                .getBlobContainerClient(serviceName)
                .generateSas(createSharedAccessPolicy(config));
        } catch (Exception e) {
            throw new UnableToGenerateSasTokenException(e);
        }
    }

    /**
     * Creates shared access policy for the given service.
     * @param config The token config
     * @return The shared access policy
     */
    private BlobServiceSasSignatureValues createSharedAccessPolicy(TokenConfig config) {

        return new BlobServiceSasSignatureValues(
            OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(config.getValidity()),
            BlobContainerSasPermission.parse(PERMISSION_WRITE_LIST)
        );
    }

    /**
     * Gets the token config for the given service.
     * @param serviceName The service name
     * @return The token config
     * @throws ServiceConfigNotFoundException If no service configuration found for the given service
     */
    private TokenConfig getTokenConfigForService(String serviceName) {
        return accessTokenProperties.getServiceConfig().stream()
            .filter(tokenConfig -> tokenConfig.getServiceName().equalsIgnoreCase(serviceName))
            .findFirst()
            .orElseThrow(
                () -> new ServiceConfigNotFoundException("No service configuration found for service " + serviceName)
            );
    }

}
