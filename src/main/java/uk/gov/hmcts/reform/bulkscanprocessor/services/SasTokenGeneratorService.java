package uk.gov.hmcts.reform.bulkscanprocessor.services;

import com.microsoft.azure.storage.blob.ContainerSASPermission;
import com.microsoft.azure.storage.blob.SASProtocol;
import com.microsoft.azure.storage.blob.ServiceSASSignatureValues;
import com.microsoft.azure.storage.blob.SharedKeyCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.config.AccessTokenProperties;
import uk.gov.hmcts.reform.bulkscanprocessor.config.AccessTokenProperties.TokenConfig;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ServiceConfigNotFoundException;

import java.time.OffsetDateTime;

@EnableConfigurationProperties(AccessTokenProperties.class)
@Service
public class SasTokenGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(SasTokenGeneratorService.class);

    private final SharedKeyCredentials sharedKeyCredentials;
    private final AccessTokenProperties accessTokenProperties;

    public SasTokenGeneratorService(
        SharedKeyCredentials sharedKeyCredentials,
        AccessTokenProperties accessTokenProperties
    ) {
        this.sharedKeyCredentials = sharedKeyCredentials;
        this.accessTokenProperties = accessTokenProperties;
    }

    public String generateSasToken(String serviceName) {
        log.info("SAS Token request received for service {} ", serviceName);
        TokenConfig config = getTokenConfigForService(serviceName);

        return new ServiceSASSignatureValues()
            .withProtocol(SASProtocol.HTTPS_HTTP)
            .withExpiryTime(OffsetDateTime.now().plusSeconds(config.getValidity()))
            .withContainerName(serviceName)
            .withPermissions(new ContainerSASPermission().withWrite(true).withList(true).toString())
            .generateSASQueryParameters(sharedKeyCredentials)
            .encode();
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
