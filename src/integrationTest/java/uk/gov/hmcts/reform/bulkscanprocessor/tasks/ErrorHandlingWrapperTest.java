package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageCredentials;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.DockerComposeContainer;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptionhandlers.TaskErrorHandler;
import uk.gov.hmcts.reform.bulkscanprocessor.services.wrapper.ErrorHandlingWrapper;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ErrorHandlingWrapperTest {
    private CloudBlobContainer testContainer;

    private static DockerComposeContainer dockerComposeContainer;

    @Autowired
    private TaskErrorHandler taskErrorHandler;

    private ErrorHandlingWrapper errorHandlingWrapper;

    private CloudBlobClient cloudBlobClient;

    @Before
    public void setUp() throws Exception {
        CloudStorageAccount account = CloudStorageAccount.parse("UseDevelopmentStorage=true");
        cloudBlobClient = account.createCloudBlobClient();

        testContainer = cloudBlobClient.getContainerReference("test");
        testContainer.createIfNotExists();

        errorHandlingWrapper = new ErrorHandlingWrapper(taskErrorHandler);
    }

    @BeforeClass
    public static void initialize() {
        File dockerComposeFile = new File("src/integrationTest/resources/docker-compose.yml");

        dockerComposeContainer = new DockerComposeContainer(dockerComposeFile)
            .withExposedService("azure-storage", 10000);

        dockerComposeContainer.start();
    }

    @After
    public void cleanUp() throws Exception {
        testContainer.deleteIfExists();
    }

    @AfterClass
    public static void tearDownContainer() {
        dockerComposeContainer.stop();
    }

    @Test
    public void should_result_in_storage_exception_and_return_null_when_acquiring_lease_on_non_existing_blob() {
        assertThat(errorHandlingWrapper.wrapAcquireLeaseFailure("test", "test",
            () -> {
                CloudBlockBlob blockBlobReference = testContainer.getBlockBlobReference("doesnotexist");
                blockBlobReference.acquireLease();
                return blockBlobReference;
            })).isNull();
    }

    @Test
    public void should_result_in_illlegal_argument_exception_and_return_null_when_parsing_storage_credentials() {
        assertThat(errorHandlingWrapper.wrapAcquireLeaseFailure("test", "test",
            () -> StorageCredentials.tryParseCredentials("invalidconnstring"))).isNull();
    }
}
