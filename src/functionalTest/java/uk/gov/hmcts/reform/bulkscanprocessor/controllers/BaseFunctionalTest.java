package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import com.azure.core.http.HttpClient;
import com.azure.core.http.ProxyOptions;
import com.azure.core.http.ProxyOptions.Type;
import com.azure.core.http.netty.NettyAsyncHttpClientBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.specialized.BlobLeaseClientBuilder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import uk.gov.hmcts.reform.bulkscanprocessor.TestHelper;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.EnvelopeResponse;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;
import uk.gov.hmcts.reform.bulkscanprocessor.services.storage.LeaseClientProvider;

import static com.jayway.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;

public abstract class BaseFunctionalTest {

    protected String testUrl;
    protected long scanDelay;
    protected String s2sUrl;
    protected String s2sName;
    protected String s2sSecret;
    protected BlobContainerClient inputContainer;
    protected BlobContainerClient rejectedContainer;
    protected String proxyHost;
    protected String proxyPort;
    protected boolean isProxyEnabled;
    protected TestHelper testHelper = new TestHelper();
    protected Config config;
    protected boolean fluxFuncTest;
    protected LeaseClientProvider leaseClientProvider;

    public void setUp() throws Exception {
        this.config = ConfigFactory.load();
        this.testUrl = config.getString("test-url");
        this.scanDelay = Long.parseLong(config.getString("test-scan-delay"));
        this.s2sUrl = config.getString("test-s2s-url");
        this.s2sName = config.getString("test-s2s-name");
        this.s2sSecret = config.getString("test-s2s-secret");
        this.proxyHost = config.getString("storage-proxy-host");
        this.proxyPort = config.getString("storage-proxy-port");
        this.isProxyEnabled = Boolean.valueOf(config.getString("proxyout.enabled"));
        this.fluxFuncTest = config.getBoolean("flux-func-test");

        String connectionString = String.format(
            "DefaultEndpointsProtocol=https;BlobEndpoint=%s;AccountName=%s;AccountKey=%s",
            config.getString("test-storage-account-url"),
            config.getString("test-storage-account-name"),
            config.getString("test-storage-account-key")
        );
        leaseClientProvider =  blobClient -> new BlobLeaseClientBuilder().blobClient(blobClient).buildClient();
        BlobServiceClientBuilder blobServiceClientBuilder = new BlobServiceClientBuilder()
            .connectionString(connectionString);

        // Apply proxy for functional tests for all environments except preview
        // as due to NSG config it has to go through outbound proxy
        if (isProxyEnabled) {
            HttpClient httpClient = new NettyAsyncHttpClientBuilder()
                .proxy(
                    new ProxyOptions(
                        Type.HTTP,
                        new InetSocketAddress(
                            proxyHost,
                            Integer.parseInt(proxyPort)
                        )
                    )
                )
                .build();

            blobServiceClientBuilder.httpClient(httpClient);
        }

        BlobServiceClient blobServiceClient = blobServiceClientBuilder.buildClient();

        String inputContainerName = config.getString("test-storage-container-name");
        String rejectedContainerName = inputContainerName + "-rejected";

        inputContainer = blobServiceClient.getBlobContainerClient(inputContainerName);
        rejectedContainer = blobServiceClient.getBlobContainerClient(rejectedContainerName);
    }

    protected void uploadZipFile(List<String> files, String metadataFile, String destZipFilename) {
        // valid zip file
        testHelper.uploadZipFile(
            inputContainer,
            files,
            metadataFile,
            destZipFilename
        );
    }

    protected EnvelopeResponse waitForEnvelopeToBeInStatus(String fileName, List<Status> awaitedStatuses) {
        String s2sToken = testHelper.s2sSignIn(this.s2sName, this.s2sSecret, this.s2sUrl);

        await("File " + fileName + " should be processed")
            .atMost(scanDelay + 40_000, TimeUnit.MILLISECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .until(() ->
                       testHelper.getEnvelopeByZipFileName(testUrl, s2sToken, fileName)
                           .filter(env -> awaitedStatuses.contains(env.getStatus()))
                           .isPresent()
            );

        EnvelopeResponse envelope = testHelper.getEnvelopeByZipFileName(testUrl, s2sToken, fileName).get();
        assertThat(envelope.getStatus()).isIn(awaitedStatuses);

        return envelope;
    }
}
