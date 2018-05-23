package uk.gov.hmcts.reform.bulkscanning.config;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageCredentials;
import com.microsoft.azure.storage.StorageCredentialsAccountAndKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URISyntaxException;

@Configuration
public class StorageConfiguration {

    @Bean
    public StorageCredentials getStorageCredentials(
        @Value("${storage.account_name}") String accountName,
        @Value("${storage.key}") String key
    ) {
        return new StorageCredentialsAccountAndKey(accountName, key);
    }

    @Bean
    public CloudStorageAccount getCloudStorageAccount(StorageCredentials storageCredentials) throws URISyntaxException {
        return new CloudStorageAccount(storageCredentials, true);
    }
}
