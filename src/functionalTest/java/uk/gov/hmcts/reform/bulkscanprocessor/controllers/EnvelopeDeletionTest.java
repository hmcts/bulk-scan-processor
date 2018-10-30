package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import com.microsoft.azure.storage.blob.BlockBlobURL;
import com.microsoft.azure.storage.blob.ContainerURL;
import com.microsoft.azure.storage.blob.models.StorageErrorException;
import com.microsoft.rest.v2.Context;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

public class EnvelopeDeletionTest {

    private transient long scanDelay;
    private transient ContainerURL testContainer;
    private List<String> filesToDeleteAfterTest = new ArrayList<>();
    private transient TestHelper testHelper;

    @Before
    public void setUp() throws Exception {
        Config conf = ConfigFactory.load();
        this.scanDelay = Long.parseLong(conf.getString("test-scan-delay"));

        this.testHelper = new TestHelper(
            conf.getString("test-storage-account-name"),
            conf.getString("test-storage-account-key"),
            conf.getString("test-storage-account-url")
        );

        testContainer = testHelper.getServiceURL().createContainerURL("bulkscan");
    }

    @After
    public void tearDown() {
        for (String filename : filesToDeleteAfterTest) {
            BlockBlobURL blockBlobURL = testContainer.createBlockBlobURL(filename);
            try {
                blockBlobURL
                    .breakLease(0, null, Context.NONE);
            } catch (StorageErrorException e) {
                // Do nothing as the file was not leased
            }
            blockBlobURL.delete(null, null, Context.NONE)
            .blockingGet();
        }
    }

    @Test
    public void should_delete_zip_file_after_successful_ingestion() throws Exception {
        List<String> files = Arrays.asList("1111006.pdf");
        String metadataFile = "1111006.metadata.json";
        String destZipFilename = testHelper.getRandomFilename("24-06-2018-00-00-00.test.zip");

        testHelper.uploadZipFile(testContainer, files, metadataFile, destZipFilename); // valid zip file
        filesToDeleteAfterTest.add(destZipFilename);

        await("file should be deleted")
            .atMost(scanDelay + 40_000, TimeUnit.MILLISECONDS)
            .pollInterval(2, TimeUnit.SECONDS)
            .until(() -> testHelper.storageHasFile(testContainer, destZipFilename), is(false));

        assertThat(testHelper.storageHasFile(testContainer, destZipFilename)).isFalse();
    }

    @Test
    public void should_keep_zip_file_after_failed_processing() throws Exception {
        String destZipFilename = testHelper.getRandomFilename("24-06-2018-00-00-00.test.zip");

        testHelper.uploadZipFile(
            testContainer,
            Arrays.asList("1111006.pdf"),
            null, // missing metadata file
            destZipFilename
        );

        filesToDeleteAfterTest.add(destZipFilename);

        await("file should not be deleted")
            .atMost(scanDelay + 15_000, TimeUnit.MILLISECONDS)
            .pollDelay(scanDelay * 2, TimeUnit.MILLISECONDS)
            .until(() -> testHelper.storageHasFile(testContainer, destZipFilename), is(true));
    }
}
