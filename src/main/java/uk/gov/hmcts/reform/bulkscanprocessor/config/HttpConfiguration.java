package uk.gov.hmcts.reform.bulkscanprocessor.config;

import com.azure.core.http.HttpClient;
import com.azure.core.http.ProxyOptions;
import com.azure.core.http.netty.NettyAsyncHttpClientBuilder;
import feign.Client;
import feign.httpclient.ApacheHttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.document.DocumentManagementClientAutoConfiguration;

import java.net.InetSocketAddress;

@AutoConfigureBefore(DocumentManagementClientAutoConfiguration.class)
@Configuration
public class HttpConfiguration {

    @Bean
    public Client getFeignHttpClient() {
        return new ApacheHttpClient(getHttpClient());
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate(clientHttpRequestFactory());
    }

    @Bean
    public HttpComponentsClientHttpRequestFactory clientHttpRequestFactory() {
        return new HttpComponentsClientHttpRequestFactory(getHttpClient());
    }

    private CloseableHttpClient getHttpClient() {
        RequestConfig config = RequestConfig.custom()
            .setConnectTimeout(30000)
            .setConnectionRequestTimeout(30000)
            .setSocketTimeout(60000)
            .build();

        return HttpClientBuilder
            .create()
            .useSystemProperties()
            .setDefaultRequestConfig(config)
            .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "storage", name = "proxy_enabled", havingValue = "true")
    public HttpClient azureHttpClientWithProxy(
        @Value("${proxy.host-name}") String proxyHostName,
        @Value("${proxy.port}") int proxyPort
    ) {
        return new NettyAsyncHttpClientBuilder()
            .proxy(
                new ProxyOptions(
                    ProxyOptions.Type.HTTP,
                    new InetSocketAddress(
                        proxyHostName,
                        proxyPort
                    )
                )
            )
            .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "storage", name = "proxy_enabled", havingValue = "false")
    public HttpClient azureHttpClientWithoutProxy() {
        return new NettyAsyncHttpClientBuilder().build();
    }

}
