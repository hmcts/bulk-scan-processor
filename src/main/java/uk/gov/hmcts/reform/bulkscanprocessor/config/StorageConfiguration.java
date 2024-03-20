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

    /**
     * Get the storage client.
     * @param accountName The account name
     * @param key The key
     * @param url The URL
     * @param httpClient The HttpClient
     * @return The BlobServiceClient
     */
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

        return new BlobServiceClientBuilder()
            .connectionString(connectionString)
            .httpClient(httpClient)
            .buildClient();
    }

    /**
     * Get the lease client provider.
     * @return The LeaseClientProvider
     */
    @Bean
    public LeaseClientProvider getLeaseClientProvider() {
        return blobClient -> new BlobLeaseClientBuilder().blobClient(blobClient).buildClient();
    }
}
