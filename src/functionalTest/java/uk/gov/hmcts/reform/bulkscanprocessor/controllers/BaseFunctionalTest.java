package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import com.google.common.collect.ImmutableList;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.OperationContext;
import com.microsoft.azure.storage.StorageCredentialsAccountAndKey;
import com.microsoft.azure.storage.StorageUri;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;

public abstract class BaseFunctionalTest {

    protected String testUrl;
    protected long scanDelay;
    protected String s2sUrl;
    protected String s2sName;
    protected String s2sSecret;
    protected String testPrivateKeyDer;
    protected CloudBlobContainer inputContainer;
    protected CloudBlobContainer rejectedContainer;
    protected String proxyHost;
    protected String proxyPort;
    protected boolean isProxyEnabled;
    protected TestHelper testHelper = new TestHelper();
    protected Config config;
    protected OperationContext operationContext;

    public void setUp() throws Exception {
        this.config = ConfigFactory.load();
        this.testUrl = config.getString("test-url");
        this.scanDelay = Long.parseLong(config.getString("test-scan-delay"));
        this.s2sUrl = config.getString("test-s2s-url");
        this.s2sName = config.getString("test-s2s-name");
        this.s2sSecret = config.getString("test-s2s-secret");
        this.testPrivateKeyDer = config.getString("test-private-key-der");
        this.proxyHost = config.getString("storage-proxy-host");
        this.proxyPort = config.getString("storage-proxy-port");
        this.isProxyEnabled = Boolean.valueOf(config.getString("proxyout.enabled"));

        StorageCredentialsAccountAndKey storageCredentials =
            new StorageCredentialsAccountAndKey(
                config.getString("test-storage-account-name"),
                config.getString("test-storage-account-key")
            );

        // Apply proxy for functional tests for all environments except preview
        // as due to NSG config it has to go through outbound proxy
        if (isProxyEnabled) {
            Proxy proxy = new Proxy(
                Proxy.Type.HTTP,
                new InetSocketAddress(proxyHost, Integer.parseInt(proxyPort))
            );

            operationContext = new OperationContext();
            operationContext.setProxy(proxy);

            // This is set temporary and will be removed/modified in subsequent PRs
            System.setProperty("http.proxyHost", proxyHost);
            System.setProperty("http.proxyPort", proxyPort);

            System.setProperty("https.proxyHost", proxyHost);
            System.setProperty("https.proxyPort", proxyPort);
        }

        CloudBlobClient cloudBlobClient = new CloudStorageAccount(
            storageCredentials,
            new StorageUri(new URI(config.getString("test-storage-account-url")), null),
            null,
            null
        )
            .createCloudBlobClient();

        String inputContainerName = config.getString("test-storage-container-name");
        String rejectedContainerName = inputContainerName + "-rejected";

        inputContainer = cloudBlobClient.getContainerReference(inputContainerName);
        rejectedContainer = cloudBlobClient.getContainerReference(rejectedContainerName);
    }

    protected void waitForFileToBeProcessed(String fileName, String s2sToken) {
        await("processing of file " + fileName + "should end")
            .atMost(scanDelay + 40_000, TimeUnit.MILLISECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .until(() -> testHelper.getEnvelopeByZipFileName(testUrl, s2sToken, fileName)
                .filter(env ->
                    ImmutableList.of(Status.NOTIFICATION_SENT, Status.COMPLETED)
                        .contains(env.getStatus())
                )
                .isPresent()
            );
    }
}
