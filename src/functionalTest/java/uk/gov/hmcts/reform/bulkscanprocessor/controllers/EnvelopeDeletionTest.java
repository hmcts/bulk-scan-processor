package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.core.PathUtility;
import io.restassured.RestAssured;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.stream.StreamSupport;

import static com.jayway.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;

@RunWith(SpringRunner.class)
@TestPropertySource("classpath:application.properties")
public class EnvelopeDeletionTest {

    @Value("${test-url}")
    private transient String testUrl;

    @Value("${scan-delay}")
    private transient int scanDelay;

    @Value("${storage-account-name}")
    private transient String accountName;

    private transient CloudBlobContainer testContainer;

    @Before
    public void setUp() throws Exception {
        String containerName = "test";
        String sasToken = RestAssured
            .given()
            .relaxedHTTPSValidation()
            .when()
            .get(testUrl + "/token/" + containerName)
            .thenReturn()
            .jsonPath()
            .getString("sas_token");

        URI containerUri = new URI("https://" + accountName + ".blob.core.windows.net/" + containerName);
        testContainer = new CloudBlobContainer(PathUtility.addToQuery(containerUri, sasToken));
    }

    @Test
    public void should_delete_zip_file_after_successful_ingestion() throws Exception {
        CloudBlockBlob zipFile = uploadZipFile("4_24-06-2018-00-00-00.zip"); // valid zip file

        await()
            .atMost(scanDelay + 1000, TimeUnit.MILLISECONDS)
            .pollDelay(5, TimeUnit.SECONDS)
            .until(zipFile::exists, is(false));
    }

    @Test
    public void should_keep_zip_file_after_failed_processing() throws Exception {
        CloudBlockBlob zipFile = uploadZipFile("2_24-06-2018-00-00-00.zip"); // invalid due to missing json file

        await()
            .timeout(scanDelay + 1000, TimeUnit.MILLISECONDS)
            .until(zipFile::exists, is(true));
    }

    private CloudBlockBlob uploadZipFile(final String zipName) throws Exception {
        String zipPath = new File("src/integrationTest/resources/" + zipName).getAbsolutePath();

        CloudBlockBlob blob = testContainer.getBlockBlobReference(zipName);
        blob.uploadFromFile(zipPath);
        return blob;
    }

    private boolean storageHasFile(String fileName) {
        return StreamSupport.stream(testContainer.listBlobs().spliterator(), false)
            .anyMatch(listBlobItem -> listBlobItem.getUri().getPath().contains(fileName));
    }
}
