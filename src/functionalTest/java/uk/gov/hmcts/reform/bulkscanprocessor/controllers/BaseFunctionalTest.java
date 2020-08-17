package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import com.azure.core.http.HttpClient;
import com.azure.core.http.ProxyOptions;
import com.azure.core.http.ProxyOptions.Type;
import com.azure.core.http.netty.NettyAsyncHttpClientBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.specialized.BlobLeaseClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import uk.gov.hmcts.reform.bulkscanprocessor.TestHelper;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.EnvelopeResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.services.storage.LeaseClientProvider;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.bulkscanprocessor.config.Configs.IS_PROXY_ENABLED;
import static uk.gov.hmcts.reform.bulkscanprocessor.config.Configs.PROXY_HOST;
import static uk.gov.hmcts.reform.bulkscanprocessor.config.Configs.PROXY_PORT;
import static uk.gov.hmcts.reform.bulkscanprocessor.config.Configs.S2S_NAME;
import static uk.gov.hmcts.reform.bulkscanprocessor.config.Configs.S2S_SECRET;
import static uk.gov.hmcts.reform.bulkscanprocessor.config.Configs.S2S_URL;
import static uk.gov.hmcts.reform.bulkscanprocessor.config.Configs.SCAN_DELAY;
import static uk.gov.hmcts.reform.bulkscanprocessor.config.Configs.STORAGE_ACCOUNT_KEY;
import static uk.gov.hmcts.reform.bulkscanprocessor.config.Configs.STORAGE_ACCOUNT_NAME;
import static uk.gov.hmcts.reform.bulkscanprocessor.config.Configs.STORAGE_ACCOUNT_URL;
import static uk.gov.hmcts.reform.bulkscanprocessor.config.Configs.STORAGE_CONTAINER_NAME;
import static uk.gov.hmcts.reform.bulkscanprocessor.config.Configs.TEST_URL;

public abstract class BaseFunctionalTest {

    protected BlobContainerClient inputContainer;
    protected BlobContainerClient rejectedContainer;
    protected TestHelper testHelper = new TestHelper();
    protected LeaseClientProvider leaseClientProvider;

    public void setUp() throws Exception {

        leaseClientProvider =  blobClient -> new BlobLeaseClientBuilder().blobClient(blobClient).buildClient();

        StorageSharedKeyCredential storageCredentials =
            new StorageSharedKeyCredential(
                STORAGE_ACCOUNT_NAME,
                STORAGE_ACCOUNT_KEY
            );

        BlobServiceClientBuilder blobServiceClientBuilder = new BlobServiceClientBuilder()
            .credential(storageCredentials)
            .endpoint(STORAGE_ACCOUNT_URL);

        // Apply proxy for functional tests for all environments except preview
        // as due to NSG config it has to go through outbound proxy
        if (IS_PROXY_ENABLED) {
            HttpClient httpClient = new NettyAsyncHttpClientBuilder()
                .proxy(
                    new ProxyOptions(
                        Type.HTTP,
                        new InetSocketAddress(
                            PROXY_HOST,
                            Integer.parseInt(PROXY_PORT)
                        )
                    )
                )
                .build();

            blobServiceClientBuilder.httpClient(httpClient);
        }

        BlobServiceClient blobServiceClient = blobServiceClientBuilder.buildClient();

        String inputContainerName = STORAGE_CONTAINER_NAME;
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
        String s2sToken = testHelper.s2sSignIn(S2S_NAME, S2S_SECRET, S2S_URL);

        await("File " + fileName + " should be processed")
            .atMost(SCAN_DELAY + 40_000, TimeUnit.MILLISECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .until(() ->
                       testHelper.getEnvelopeByZipFileName(TEST_URL, s2sToken, fileName)
                           .filter(env -> awaitedStatuses.contains(env.getStatus()))
                           .isPresent()
            );

        EnvelopeResponse envelope = testHelper.getEnvelopeByZipFileName(TEST_URL, s2sToken, fileName).get();
        assertThat(envelope.getStatus()).isIn(awaitedStatuses);

        return envelope;
    }
}
