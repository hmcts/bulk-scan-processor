package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.ListBlobItem;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static com.jayway.awaitility.Awaitility.await;
import static com.microsoft.azure.storage.blob.BlobListingDetails.SNAPSHOTS;
import static com.microsoft.azure.storage.blob.DeleteSnapshotsOption.INCLUDE_SNAPSHOTS;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

public class EnvelopeDeletionTest extends BaseFunctionalTest {

    private List<String> filesToDeleteAfterTest = new ArrayList<>();

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        for (String filename : filesToDeleteAfterTest) {
            try {
                inputContainer.getBlockBlobReference(filename).breakLease(0);
            } catch (StorageException e) {
                // Do nothing as the file was not leased
            }

            inputContainer.getBlockBlobReference(filename).deleteIfExists(INCLUDE_SNAPSHOTS, null, null, null);
            rejectedContainer.getBlockBlobReference(filename).deleteIfExists(INCLUDE_SNAPSHOTS, null, null, null);
        }
    }

    @Test
    public void should_move_invalid_zip_file_to_rejected_container() throws Exception {
        String destZipFilename = testHelper.getRandomFilename("24-06-2018-00-00-00.test.zip");

        testHelper.uploadZipFile(
            inputContainer,
            Arrays.asList("1111006.pdf"),
            null, // missing metadata file
            destZipFilename,
            testPrivateKeyDer
        );

        filesToDeleteAfterTest.add(destZipFilename);

        await("file should be deleted")
            .atMost(scanDelay + 40_000, TimeUnit.MILLISECONDS)
            .pollInterval(2, TimeUnit.SECONDS)
            .until(() -> testHelper.storageHasFile(inputContainer, destZipFilename), is(false));

        assertThat(testHelper.storageHasFile(rejectedContainer, destZipFilename)).isTrue();
        assertThat(searchByName(rejectedContainer, destZipFilename)).hasSize(1);
    }

    @Test
    public void should_create_a_snapshot_of_previously_rejected_file_if_its_sent_again() throws Exception {
        // given
        final int numberOfUploads = 2;

        String fileName = testHelper.getRandomFilename("24-06-2018-00-00-00.test.zip");
        filesToDeleteAfterTest.add(fileName);

        // when
        times(numberOfUploads, () -> {

            testHelper.uploadZipFile(
                inputContainer,
                Arrays.asList("1111006.pdf"),
                null, // missing metadata file
                fileName,
                testPrivateKeyDer
            );

            await("file should be deleted")
                .atMost(scanDelay + 40_000, TimeUnit.MILLISECONDS)
                .pollInterval(2, TimeUnit.SECONDS)
                .until(() -> testHelper.storageHasFile(inputContainer, fileName), is(false));
        });

        // then
        assertThat(searchByName(rejectedContainer, fileName))
            .as("Should create {} snapshots", numberOfUploads)
            .hasSize(numberOfUploads);
    }

    private void times(int times, Runnable actionToRun) {
        IntStream.range(0, times).forEach(index -> actionToRun.run());
    }

    private List<ListBlobItem> searchByName(CloudBlobContainer container, String fileName) {
        Iterable<ListBlobItem> blobs = container.listBlobs(null, true, EnumSet.of(SNAPSHOTS), null, null);

        return stream(blobs.spliterator(), false)
            .filter(b -> b.getUri().getPath().contains(fileName))
            .collect(toList());
    }
}
