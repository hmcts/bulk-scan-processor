package uk.gov.hmcts.reform.bulkscanprocessor.config;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageCredentialsAccountAndKey;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URISyntaxException;

@Configuration
public class StorageConfiguration {

    @Bean
    public CloudBlobClient getCloudBlobClient(
        @Value("${storage.account_name}") String accountName,
        @Value("${storage.key}") String key
    ) throws URISyntaxException {

        return new CloudStorageAccount(
            new StorageCredentialsAccountAndKey(accountName, key),
            true
        ).createCloudBlobClient();
    }
}
