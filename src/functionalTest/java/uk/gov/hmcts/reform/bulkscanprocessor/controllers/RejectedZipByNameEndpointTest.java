package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import com.azure.core.util.Context;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.DeleteSnapshotsOptionType;
import com.azure.storage.blob.models.ListBlobsOptions;
import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static io.restassured.RestAssured.given;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static uk.gov.hmcts.reform.bulkscanprocessor.config.TestConfiguration.SCAN_DELAY;
import static uk.gov.hmcts.reform.bulkscanprocessor.config.TestConfiguration.TEST_URL;

public class RejectedZipByNameEndpointTest extends BaseFunctionalTest {

    private List<String> filesToDeleteAfterTest = new ArrayList<>();

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
    }

    @AfterEach
    public void tearDown() {
        for (String filename : filesToDeleteAfterTest) {
            var inBlobClient = inputContainer.getBlobClient(filename);
            if (inBlobClient.exists()) {
                inBlobClient.deleteWithResponse(DeleteSnapshotsOptionType.INCLUDE, null, null, Context.NONE);
            }

            var rejBlobClient = rejectedContainer.getBlobClient(filename);

            if (rejBlobClient.exists()) {
                rejBlobClient.deleteWithResponse(DeleteSnapshotsOptionType.INCLUDE, null, null, Context.NONE);
            }
        }
    }

    @Test
    public void should_return_rejected_zip_files_by_name() {
        String destZipFilename = testHelper.getRandomFilename();

        testHelper.uploadZipFile(
            inputContainer,
            Arrays.asList("1111006.pdf"),
            null, // missing metadata file
            destZipFilename
        );

        filesToDeleteAfterTest.add(destZipFilename);

        await(destZipFilename + " file should be deleted")
            .atMost(SCAN_DELAY + 60_000, TimeUnit.MILLISECONDS)
            .pollInterval(2, TimeUnit.SECONDS)
            .until(() -> testHelper.storageHasFile(inputContainer, destZipFilename), is(false));

        assertThat(testHelper.storageHasFile(rejectedContainer, destZipFilename)).isTrue();
        assertThat(searchByName(rejectedContainer, destZipFilename)).hasSize(1);

        given()
            .baseUri(TEST_URL)
            .relaxedHTTPSValidation()
            .get("/reports/rejected-zip-files/name/" + destZipFilename)
            .then().statusCode(200)
            .body("rejected_zip_files:event", equalTo("FILE_VALIDATION_FAILURE"));

    }

    @Test
    public void should_return_empty_list_if_there_is_no_rejected_zip_files() {

        String destZipFilename = testHelper.getRandomFilename();
        RestAssured
            .given()
            .baseUri(TEST_URL)
            .relaxedHTTPSValidation()
            .get("/reports/rejected-zip-files/name/" + destZipFilename)
            .then().statusCode(200)
            .assertThat()
            .body("count", equalTo(0));

    }

    private List<BlobItem> searchByName(BlobContainerClient container, String fileName) {
        ListBlobsOptions listOptions = new ListBlobsOptions();
        listOptions.getDetails().setRetrieveSnapshots(true);
        listOptions.setPrefix(fileName);
        return container.listBlobs(listOptions, null, null)
            .stream()
            .collect(toList());
    }

}
