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

@EnableConfigurationProperties(AccessTokenProperties.class)
@Service
public class SasTokenGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(SasTokenGeneratorService.class);

    private final BlobServiceClient blobServiceClient;
    private final AccessTokenProperties accessTokenProperties;
    private static final String PERMISSION_WRITE_LIST = "racwdltm";

    public SasTokenGeneratorService(
        BlobServiceClient blobServiceClient,
        AccessTokenProperties accessTokenProperties
    ) {
        this.blobServiceClient = blobServiceClient;
        this.accessTokenProperties = accessTokenProperties;
    }

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

    private BlobServiceSasSignatureValues createSharedAccessPolicy(TokenConfig config) {

        return new BlobServiceSasSignatureValues(
            OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(config.getValidity()),
            BlobContainerSasPermission.parse(PERMISSION_WRITE_LIST)
        );
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
