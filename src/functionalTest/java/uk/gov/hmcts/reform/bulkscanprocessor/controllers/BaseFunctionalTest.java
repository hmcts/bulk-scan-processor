package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageCredentialsAccountAndKey;
import com.microsoft.azure.storage.StorageUri;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.net.URI;

public abstract class BaseFunctionalTest {

    protected String testUrl;
    protected long scanDelay;
    protected String s2sUrl;
    protected String s2sName;
    protected String s2sSecret;
    protected String testPrivateKeyDer;
    protected CloudBlobContainer inputContainer;
    protected CloudBlobContainer rejectedContainer;
    protected TestHelper testHelper = new TestHelper();
    protected Config config;

    public void setUp() throws Exception {
        this.config = ConfigFactory.load();
        this.testUrl = config.getString("test-url");
        this.scanDelay = Long.parseLong(config.getString("test-scan-delay"));
        this.s2sUrl = config.getString("test-s2s-url");
        this.s2sName = config.getString("test-s2s-name");
        this.s2sSecret = config.getString("test-s2s-secret");
        this.testPrivateKeyDer = config.getString("test-private-key-der");

        StorageCredentialsAccountAndKey storageCredentials =
            new StorageCredentialsAccountAndKey(
                config.getString("test-storage-account-name"),
                config.getString("test-storage-account-key")
            );

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
}
