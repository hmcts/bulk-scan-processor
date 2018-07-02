package uk.gov.hmcts.reform.bulkscanprocessor.features;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.palantir.docker.compose.DockerComposeRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.services.BlobStorageRead;
import uk.gov.hmcts.reform.bulkscanprocessor.services.PDF;

import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@RunWith(MockitoJUnitRunner.class)
public class BlobStorageReadTest {

    @ClassRule
    public static DockerComposeRule docker = DockerComposeRule.builder()
        .file("src/integrationTest/resources/docker-compose.yml")
        .build();

    @Mock
    private Consumer<List<PDF>> pdfsConsumer;

    private CloudBlobClient cloudBlobClient;

    @Before
    public void setup() throws URISyntaxException, InvalidKeyException, StorageException {
        CloudStorageAccount account = CloudStorageAccount.parse("UseDevelopmentStorage=true");
        cloudBlobClient = account.createCloudBlobClient();
    }

    @Test
    public void connects_without_exceptions() throws URISyntaxException, StorageException {
        CloudBlobContainer abc = cloudBlobClient.getContainerReference("abc");
        abc.createIfNotExists();

        Throwable throwable = catchThrowable(() -> {
            BlobStorageRead blobStorageRead = new BlobStorageRead(cloudBlobClient, pdfsConsumer);
            blobStorageRead.readBlobs();
        });

        assertThat(throwable).doesNotThrowAnyException();
    }
}
