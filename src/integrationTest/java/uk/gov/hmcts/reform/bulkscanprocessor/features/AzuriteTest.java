package uk.gov.hmcts.reform.bulkscanprocessor.features;

import com.microsoft.azure.storage.blob.ContainerURL;
import com.microsoft.azure.storage.blob.PipelineOptions;
import com.microsoft.azure.storage.blob.ServiceURL;
import com.microsoft.azure.storage.blob.SharedKeyCredentials;
import com.microsoft.azure.storage.blob.StorageException;
import com.microsoft.azure.storage.blob.StorageURL;
import com.microsoft.azure.storage.blob.models.AccountKind;
import com.microsoft.rest.v2.Context;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testcontainers.containers.DockerComposeContainer;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.InvalidKeyException;

import static org.assertj.core.api.Assertions.assertThat;

public class AzuriteTest {

    private static DockerComposeContainer dockerComposeContainer;
    private ContainerURL incoming;

    @BeforeClass
    public static void initialize() {
        File dockerComposeFile = new File("src/integrationTest/resources/docker-compose.yml");

        dockerComposeContainer = new DockerComposeContainer(dockerComposeFile)
            .withExposedService("azure-storage", 10000);

        dockerComposeContainer.start();
    }

    @AfterClass
    public static void tearDownContainer() {
        dockerComposeContainer.stop();
    }

    @Before
    public void setup() throws URISyntaxException, InvalidKeyException, StorageException, MalformedURLException {

        SharedKeyCredentials credential = new SharedKeyCredentials("test", "blah");
        ServiceURL serviceURL = new ServiceURL(new URL("http://127.0.0.1"), StorageURL
            .createPipeline(credential, new PipelineOptions()));

        incoming = serviceURL.createContainerURL("incoming");
        incoming.create(null, null, Context.NONE)
            .blockingGet();

        // TODO look at dev storage
//        CloudStorageAccount account = CloudStorageAccount.parse("UseDevelopmentStorage=true");
    }

    @Test
    public void testAzuriteIntegration() throws StorageException {
        AccountKind accountKind = incoming.getAccountInfo(Context.NONE)
            .blockingGet()
            .headers()
            .accountKind();

        assertThat(accountKind).isEqualTo(AccountKind.STORAGE_V2);
    }
}
