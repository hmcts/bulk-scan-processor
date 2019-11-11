package uk.gov.hmcts.reform.bulkscanprocessor.config;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.OperationContext;
import com.microsoft.azure.storage.StorageCredentialsAccountAndKey;
import com.microsoft.azure.storage.StorageUri;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;

@Configuration
@Profile(Profiles.NOT_STORAGE_STUB)
public class StorageConfiguration {

    private static final Logger log = LoggerFactory.getLogger(StorageConfiguration.class);

    @Value("${storage.proxy_enabled}")
    private boolean isProxyEnabled;

    @Value("${storage.proxy_host}")
    private String proxyHost;

    @Value("${storage.proxy_port}")
    private int proxyPort;

    @Bean
    public CloudBlobClient getCloudBlobClient(
        @Value("${storage.account_name}") String accountName,
        @Value("${storage.key}") String key,
        @Value("${storage.url}") String url
    ) throws URISyntaxException {
        log.info("Initialising cloud blob client. Account name: {}, key: {}, url: {}", accountName, key, url);

        if (isProxyEnabled) {
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
            OperationContext.setDefaultProxy(proxy);
        }
        return new CloudStorageAccount(
            new StorageCredentialsAccountAndKey(accountName, key),
            new StorageUri(new URI(url), null),
            null,
            null
        )
            .createCloudBlobClient();
    }
}
