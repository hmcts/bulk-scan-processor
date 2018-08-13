package uk.gov.hmcts.reform.bulkscanprocessor.features;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.palantir.docker.compose.DockerComposeRule;
import com.palantir.docker.compose.connection.waiting.HealthChecks;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.net.URISyntaxException;
import java.security.InvalidKeyException;

import static org.assertj.core.api.Assertions.assertThat;

public class AzuriteTest {

    @ClassRule
    public static DockerComposeRule docker = DockerComposeRule.builder()
        .file("src/integrationTest/resources/docker-compose.yml")
        .waitingForService("azure-storage", HealthChecks.toHaveAllPortsOpen())
        .waitingForService("azure-storage", HealthChecks.toRespondOverHttp(10000, (port) -> port.inFormat("https://$HOST:$EXTERNAL_PORT")))
        .build();

    private CloudBlobContainer root;

    @Before
    public void setup() throws URISyntaxException, InvalidKeyException, StorageException {
        CloudStorageAccount account = CloudStorageAccount.parse("UseDevelopmentStorage=true");
        CloudBlobClient serviceClient = account.createCloudBlobClient();

        root = serviceClient.getContainerReference("incoming");
        root.createIfNotExists();
    }

    @Test
    public void testAzuriteIntegration() throws URISyntaxException, InvalidKeyException, StorageException {
        assertThat(root.exists()).isTrue();
    }
}
