package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.core.PathUtility;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.bulkscanprocessor.config.StorageConfiguration;

import java.io.File;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {StorageConfiguration.class})
public class WhenProcessingEnvelopeTest {

    @Value("${test-url}")
    private String testUrl;

    @Value("${scan.delay}")
    private int scanDelay;

    private String serviceName = "test";

    @Autowired
    private CloudBlobClient cloudBlobClient;

    private CloudBlobContainer testContainer;

    @Before
    public void setUp() throws Exception {
        String sasToken = obtainSASToken();
        URI containerUri = cloudBlobClient.getContainerReference(serviceName).getUri();
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
        RestTemplate restTemplate = new RestTemplate();
        ObjectNode objectNode = restTemplate.getForObject(testUrl + "/token/" + serviceName, ObjectNode.class);
        JsonNode sasToken = objectNode.get("sas_token");
        if (sasToken != null) {
            return sasToken.asText();
        }
        throw new RuntimeException("Empty SAS token");
    }

    private void waitForBlobProcess() {
        try {
            Thread.sleep(scanDelay + 1000); // extra second for a good measure...
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
