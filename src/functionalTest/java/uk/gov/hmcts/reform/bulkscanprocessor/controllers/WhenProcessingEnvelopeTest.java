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

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@TestPropertySource("classpath:application.properties")
public class WhenProcessingEnvelopeTest {

    @Value("${test-url}")
    private String testUrl;

    @Value("${scan.delay}")
    private int scanDelay;

    @Value("${storage.account_name")
    private String accountName;

    private String serviceName = "test";

    private CloudBlobContainer testContainer;

    @Before
    public void setUp() throws Exception {
        String sasToken = obtainSASToken();
        URI containerUri = new URI("https://" + accountName + ".blob.core.windows.net/" + serviceName);
        testContainer = new CloudBlobContainer(PathUtility.addToQuery(containerUri, sasToken));
    }

    @Test
    public void should_delete_zip_file_after_successful_ingestion() throws Exception {
        // upload the file
        String zipName = "4_24-06-2018-00-00-00.zip";
        String zipPath = new File("src/integrationTest/resources/" + zipName).getAbsolutePath();

        CloudBlockBlob blockBlobReference = testContainer.getBlockBlobReference(zipName);
        blockBlobReference.uploadFromFile(zipPath);

        waitForBlobProcess();

        assertThat(blockBlobReference.exists()).isFalse();
    }

    private String obtainSASToken() {
        return RestAssured
            .given()
            .relaxedHTTPSValidation()
            .when()
            .get(testUrl + "/token/" + serviceName)
            .thenReturn()
            .jsonPath()
            .getString("sas_token");
    }

    private void waitForBlobProcess() {
        try {
            Thread.sleep(scanDelay + 1000); // extra second for a good measure...
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
