package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import com.azure.core.util.Context;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.DeleteSnapshotsOptionType;
import com.azure.storage.blob.models.ListBlobsOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

public class EnvelopeDeletionTest extends BaseFunctionalTest {

    private List<String> filesToDeleteAfterTest = new ArrayList<>();

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
    }

    @AfterEach
    public void tearDown() throws Exception {
        for (String filename : filesToDeleteAfterTest) {
            BlobClient inBlobClient = inputContainer.getBlobClient(filename);
            if (inBlobClient.exists()) {
                inBlobClient.deleteWithResponse(DeleteSnapshotsOptionType.INCLUDE, null, null, Context.NONE);
            }

            BlobClient rejBlobClient = rejectedContainer.getBlobClient(filename);

            if (rejBlobClient.exists()) {
                rejBlobClient.deleteWithResponse(DeleteSnapshotsOptionType.INCLUDE, null, null,
                    Context.NONE);
            }
        }
    }

    @Test
    public void should_move_invalid_zip_file_to_rejected_container() throws Exception {
        String destZipFilename = testHelper.getRandomFilename();

        testHelper.uploadZipFile(
            inputContainer,
            Arrays.asList("1111006.pdf"),
            null, // missing metadata file
            destZipFilename
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

        String fileName = testHelper.getRandomFilename();
        filesToDeleteAfterTest.add(fileName);

        // when
        times(numberOfUploads, () -> {

            testHelper.uploadZipFile(
                inputContainer,
                Arrays.asList("1111006.pdf"),
                null, // missing metadata file
                fileName
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

    private List<BlobItem> searchByName(BlobContainerClient container, String fileName) {
        ListBlobsOptions listOptions = new ListBlobsOptions();
        listOptions.getDetails().setRetrieveSnapshots(true);
        return    container.listBlobs(listOptions,null, null)
            .stream()
                .filter(b -> b.getName().equals(fileName))
                .collect(toList());
    }
}
