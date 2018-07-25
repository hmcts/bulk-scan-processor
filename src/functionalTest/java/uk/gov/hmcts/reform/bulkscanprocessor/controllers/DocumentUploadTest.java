package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageCredentialsAccountAndKey;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.DocumentManagementService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.output.Pdf;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.google.common.io.Resources.getResource;
import static com.google.common.io.Resources.toByteArray;
import static com.jayway.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.hamcrest.Matchers.is;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Event.DOC_UPLOADED;

@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource("classpath:application.properties")
public class DocumentUploadTest {

    @Autowired
    private EnvelopeRepository envelopeRepository;

    @Autowired
    private DocumentManagementService documentManagementService;

    @Value("${test-scan-delay}")
    private long scanDelay;

    @Value("${test-storage-account-name}")
    private String accountName;

    @Value("${test-storage-account-key}")
    private String testStorageAccountKey;

    private CloudBlobContainer testContainer;


    @Before
    public void setUp() throws Exception {
        StorageCredentialsAccountAndKey storageCredentials =
            new StorageCredentialsAccountAndKey(accountName, testStorageAccountKey);

        testContainer = new CloudStorageAccount(storageCredentials, true)
            .createCloudBlobClient()
            .getContainerReference("test");
    }

    @Test
    public void should_process_document_after_upload_and_set_status_uploaded() throws Exception {
        String zipFilename = "1_24-06-2018-00-00-00.zip";
        uploadZipToBlobStore(zipFilename);

        // document is removed from storage after processing
        await()
            .atMost(scanDelay + 10000, TimeUnit.MILLISECONDS)
            .until(() -> storageHasFile(zipFilename), is(false));

        List<Envelope> envelopesFromDb = envelopeRepository.findAll();

        assertThat(envelopesFromDb.size()).isEqualTo(1);
        assertThat(envelopesFromDb)
            .extracting("zipFileName", "status")
            .containsExactlyInAnyOrder(tuple("1_24-06-2018-00-00-00.zip", DOC_UPLOADED));

        List<Pdf> docs = envelopesFromDb.stream()
            .flatMap(e -> e.getScannableItems().stream())
            .map(si -> documentManagementService.getDocument(si.getIdAsString(), si.getFileName()))
            .collect(Collectors.toList());

        assertThat(docs.size()).isEqualTo(2);
    }


    @Test
    public void should_process_document_after_upload_and_fail_on_missing_envelope() throws Exception {
        String zipFilename = "2_24-06-2018-00-00-00.zip";
        uploadZipToBlobStore(zipFilename);

        // document is removed from storage after processing
        await()
            .atMost(scanDelay + 10000, TimeUnit.MILLISECONDS)
            .until(() -> storageHasFile(zipFilename), is(false));

        List<Envelope> envelopesFromDb = envelopeRepository.findAll();

        assertThat(envelopesFromDb.size()).isEqualTo(0);
    }

    // TODO next 2 methods duplicated, refactor to test utilities
    private void uploadZipToBlobStore(String fileName) throws Exception {
        byte[] zipFile = toByteArray(getResource(fileName));

        CloudBlockBlob blockBlobReference = testContainer.getBlockBlobReference(fileName);
        blockBlobReference.uploadFromByteArray(zipFile, 0, zipFile.length);
    }

    private boolean storageHasFile(String fileName) {
        return StreamSupport.stream(testContainer.listBlobs().spliterator(), false)
            .anyMatch(listBlobItem -> listBlobItem.getUri().getPath().contains(fileName));
    }
}
