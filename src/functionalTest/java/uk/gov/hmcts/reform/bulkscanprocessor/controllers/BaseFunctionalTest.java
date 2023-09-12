package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.specialized.BlobLeaseClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import uk.gov.hmcts.reform.bulkscanprocessor.TestHelper;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.EnvelopeResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.services.storage.LeaseClientProvider;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.bulkscanprocessor.config.TestConfiguration.S2S_NAME;
import static uk.gov.hmcts.reform.bulkscanprocessor.config.TestConfiguration.S2S_SECRET;
import static uk.gov.hmcts.reform.bulkscanprocessor.config.TestConfiguration.S2S_URL;
import static uk.gov.hmcts.reform.bulkscanprocessor.config.TestConfiguration.SCAN_DELAY;
import static uk.gov.hmcts.reform.bulkscanprocessor.config.TestConfiguration.STORAGE_ACCOUNT_KEY;
import static uk.gov.hmcts.reform.bulkscanprocessor.config.TestConfiguration.STORAGE_ACCOUNT_NAME;
import static uk.gov.hmcts.reform.bulkscanprocessor.config.TestConfiguration.STORAGE_ACCOUNT_URL;
import static uk.gov.hmcts.reform.bulkscanprocessor.config.TestConfiguration.STORAGE_CONTAINER_NAME;
import static uk.gov.hmcts.reform.bulkscanprocessor.config.TestConfiguration.TEST_URL;

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

        BlobServiceClient blobServiceClient = blobServiceClientBuilder.buildClient();

        inputContainer = blobServiceClient.getBlobContainerClient(STORAGE_CONTAINER_NAME);
        rejectedContainer = blobServiceClient.getBlobContainerClient(STORAGE_CONTAINER_NAME + "-rejected");
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
            .atMost(SCAN_DELAY + 120_000, TimeUnit.MILLISECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .until(
                () -> {
                    var envelope
                        = testHelper.getEnvelopeByContainerAndFileName(TEST_URL, STORAGE_CONTAINER_NAME, fileName);
                    return envelope != null && awaitedStatuses.contains(envelope.getStatus());
                }
            );

        EnvelopeResponse envelope = testHelper.getEnvelopeByContainerAndFileName(
            TEST_URL,
            STORAGE_CONTAINER_NAME,
            fileName
        );

        assertThat(envelope.getStatus()).isIn(awaitedStatuses);

        return envelope;
    }
}
