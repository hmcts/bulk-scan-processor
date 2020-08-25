package uk.gov.hmcts.reform.bulkscanprocessor.config;

import com.azure.core.http.HttpClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.specialized.BlobLeaseClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import uk.gov.hmcts.reform.bulkscanprocessor.services.storage.LeaseClientProvider;

@Configuration
@Profile(Profiles.NOT_STORAGE_STUB)
public class StorageConfiguration {

    @Value("${storage.proxy_enabled}")
    private boolean isProxyEnabled;

    @Value("${storage.proxy_host}")
    private String proxyHost;

    @Value("${storage.proxy_port}")
    private int proxyPort;

    @Bean
    public BlobServiceClient getStorageClient(
        @Value("${storage.account_name}") String accountName,
        @Value("${storage.key}") String key,
        @Value("${storage.url}") String url,
        HttpClient httpClient
    ) {
        String connectionString = String.format(
            "DefaultEndpointsProtocol=https;BlobEndpoint=%s;AccountName=%s;AccountKey=%s",
            url,
            accountName,
            key
        );

        //always use proxy
        return new BlobServiceClientBuilder()
            .connectionString(connectionString)
            .httpClient(httpClient)
            .buildClient();
    }

    @Bean
    public LeaseClientProvider getLeaseClientProvider() {
        return blobClient -> new BlobLeaseClientBuilder().blobClient(blobClient).buildClient();
    }
}
