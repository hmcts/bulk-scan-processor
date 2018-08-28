package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.output.Pdf;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.google.common.io.Resources.getResource;
import static com.google.common.io.Resources.toByteArray;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@RunWith(SpringRunner.class)
@SpringBootTest
public class BlobProcessorTaskWithAcquireLeaseTest extends ProcessorTestSuite<BlobProcessorTask> {
    @Rule
    public OutputCapture outputCapture = new OutputCapture();


    @Before
    public void setUp() throws Exception {
        super.setUp(BlobProcessorTask::new);
    }

    @After
    public void tearDown() {
        outputCapture.flush();
    }

    @Test
    public void should_process_zip_file_only_once_by_processor_thread_which_acquires_lease_on_blob()
        throws Exception {
        //Given
        givenValidZipFileUploadedAndDocStoreMocked();

        //When
        Future<Void> future = processBlobUsingExecutor();

        processor.processBlobs();

        future.get(); // wait for completion of processor.processBlobs()

        //Then
        //One thread should not be able to acquire lease and other should fail
        assertThat(outputCapture.toString())
            .contains("Lease already acquired for container test and zip file 1_24-06-2018-00-00-00.zip");

        //We expect only one envelope which was uploaded and other failed
        List<Envelope> envelopes = envelopeRepository.findAll();
        assertThat(envelopes.size()).isEqualTo(1);

        // and
        List<ProcessEvent> processEvents = processEventRepository.findAll();
        assertThat(processEvents).hasSize(2);
    }

    @NotNull
    private Future<Void> processBlobUsingExecutor() {
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        return executorService.submit(() -> {
            processor.processBlobs();
            return null;
        });
    }

    private void givenValidZipFileUploadedAndDocStoreMocked() throws Exception {
        uploadZipToBlobStore(ZIP_FILE_NAME_SUCCESS);

        byte[] test1PdfBytes = toByteArray(getResource("1111001.pdf"));
        byte[] test2PdfBytes = toByteArray(getResource("1111002.pdf"));

        Pdf pdf1 = new Pdf("1111001.pdf", test1PdfBytes);
        Pdf pdf2 = new Pdf("1111002.pdf", test2PdfBytes);

        given(documentManagementService.uploadDocuments(ImmutableList.of(pdf1, pdf2)))
            .willReturn(getFileUploadResponse());
    }
}
