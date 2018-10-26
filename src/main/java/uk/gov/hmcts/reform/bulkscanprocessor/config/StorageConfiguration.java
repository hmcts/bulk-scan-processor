package uk.gov.hmcts.reform.bulkscanprocessor.config;

import com.microsoft.azure.storage.blob.PipelineOptions;
import com.microsoft.azure.storage.blob.ServiceURL;
import com.microsoft.azure.storage.blob.SharedKeyCredentials;
import com.microsoft.azure.storage.blob.StorageURL;
import com.microsoft.rest.v2.http.HttpPipeline;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidKeyException;

@Configuration
public class StorageConfiguration {

    @Bean
    public SharedKeyCredentials sharedKeyCredentials(
        @Value("${storage.account_name}") String accountName,
        @Value("${storage.key}") String key
    ) throws InvalidKeyException {
        return new SharedKeyCredentials(accountName, key);
    }

    @Bean
    public HttpPipeline httpPipeline(SharedKeyCredentials credential) {
        return StorageURL.createPipeline(credential, new PipelineOptions());
    }

    @Bean
    public ServiceURL serviceUrl(
        HttpPipeline httpPipeline,
        @Value("${storage.account_name}") String accountName,
        @Value("${storage.endpoint}") String endpoint
    ) throws MalformedURLException {
        URL url = new URL(String.format("https://%s.%s", accountName, endpoint));
        return new ServiceURL(url, httpPipeline);
    }
}
