package uk.gov.hmcts.reform.bulkscanprocessor.features;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.palantir.docker.compose.DockerComposeRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.services.DocumentProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.services.PDF;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class DocumentProcessorTest {

    @ClassRule
    public static DockerComposeRule docker = DockerComposeRule.builder()
        .file("src/integrationTest/resources/docker-compose.yml")
        .build();

    @Mock
    private Consumer<List<PDF>> pdfsConsumer;

    @Captor
    private ArgumentCaptor<List<PDF>> pdfListCaptor;

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
            DocumentProcessor documentProcessor = new DocumentProcessor(cloudBlobClient, pdfsConsumer);
            documentProcessor.readBlobs();
        });

        assertThat(throwable).doesNotThrowAnyException();
    }

    @Test
    public void consumer_is_called_for_all_pdf_files() throws URISyntaxException, StorageException, IOException {
        CloudBlobContainer abc = cloudBlobClient.getContainerReference("abc");
        abc.createIfNotExists();

        // this zip has 3 pdfs: abc.zip, def.zip ghi.zip
        String testZip = "read-pdf-files.zip";
        CloudBlockBlob blockBlobReference = abc.getBlockBlobReference(testZip);
        blockBlobReference.uploadFromFile(new File("src/integrationTest/resources/" + testZip).getAbsolutePath());

        DocumentProcessor documentProcessor = new DocumentProcessor(cloudBlobClient, pdfsConsumer);
        documentProcessor.readBlobs();

        verify(pdfsConsumer, times(1)).accept(pdfListCaptor.capture());

        assertThat(pdfListCaptor.getAllValues()).hasSize(1);

        assertThat(pdfListCaptor.getValue())
            .hasSize(3)
            .anyMatch(pdf -> "abc.pdf".equals(pdf.getFilename()))
            .anyMatch(pdf -> "def.pdf".equals(pdf.getFilename()))
            .anyMatch(pdf -> "ghi.pdf".equals(pdf.getFilename()));
    }
}
