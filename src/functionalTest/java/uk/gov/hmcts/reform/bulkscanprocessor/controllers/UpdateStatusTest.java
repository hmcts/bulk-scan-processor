package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageCredentialsAccountAndKey;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.EnvelopeResponse;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

public class UpdateStatusTest {

    private String testUrl;
    private long scanDelay;
    private String s2sUrl;
    private String s2sName;
    private String s2sSecret;
    private CloudBlobContainer testContainer;

    private TestHelper testHelper = new TestHelper();

    @Before
    public void setUp() throws Exception {
        Config conf = ConfigFactory.load();

        this.testUrl = conf.getString("test-url");
        this.scanDelay = Long.parseLong(conf.getString("test-scan-delay"));
        this.s2sUrl = conf.getString("test-s2s-url");
        this.s2sName = conf.getString("test-s2s-name");
        this.s2sSecret = conf.getString("test-s2s-secret");

        this.testContainer =
            new CloudStorageAccount(
                new StorageCredentialsAccountAndKey(
                    conf.getString("test-storage-account-name"),
                    conf.getString("test-storage-account-key")
                ),
                true
            )
                .createCloudBlobClient()
                .getContainerReference("test");
    }

    @Test
    public void should_allow_update_envelope_status() throws Exception {
        String destZipFilename = testHelper.getRandomFilename("24-06-2018-00-00-00.zip");

        testHelper.uploadZipFile(
            testContainer,
            Arrays.asList("1111006.pdf", "1111002.pdf"),
            "1111006_2.metadata.json",
            destZipFilename
        );

        await("file should be deleted")
            .atMost(scanDelay + 15_000, TimeUnit.MILLISECONDS)
            .until(() -> testHelper.storageHasFile(testContainer, destZipFilename), is(false));

        String s2sToken = testHelper.s2sSignIn(this.s2sName, this.s2sSecret, this.s2sUrl);

        // find our envelope
        UUID envelopeId =
            testHelper
                .getEnvelopes(this.testUrl, s2sToken, Status.PROCESSED)
                .envelopes
                .stream()
                .filter(e -> Objects.equals(e.getZipFileName(), destZipFilename))
                .map(e -> e.getId())
                .findFirst()
                .get();

        testHelper.updateEnvelopeStatus(
            this.testUrl,
            s2sToken,
            envelopeId,
            Status.CONSUMED
        );

        EnvelopeResponse envelopeAfterUpdate = testHelper.getEnvelope(this.testUrl, s2sToken, envelopeId);

        assertThat(envelopeAfterUpdate.getStatus())
            .as("Envelope should have status " + Status.CONSUMED)
            .isEqualTo(Status.CONSUMED);

    }
}
