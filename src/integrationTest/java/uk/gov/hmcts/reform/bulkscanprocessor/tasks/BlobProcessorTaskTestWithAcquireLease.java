package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.google.common.collect.ImmutableList;
import com.netflix.servo.util.ThreadFactories;
import org.apache.commons.lang3.StringUtils;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.google.common.io.Resources.getResource;
import static com.google.common.io.Resources.toByteArray;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;

@RunWith(SpringRunner.class)
@SpringBootTest
public class BlobProcessorTaskTestWithAcquireLease extends ProcessorTestSuite<BlobProcessorTask> {
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
        // Given
        givenValidZipFileUploadedAndDocStoreMocked();

        // When
        // 5 blob processor task threads try to process single blob
        List<Future<Void>> futureTasks = processBlobUsingExecutor(5);

        futureTasks.forEach(future -> {
            try {
                future.get(); // wait for completion of processor.processBlobs()
            } catch (Exception e) {
                fail("Failed while processing blobs with exception " + e);
            }
        });

        // Then
        // Only one thread should be able to acquire lease and others should fail
        assertThat(
            StringUtils.countMatches(
                outputCapture.toString(),
                "Lease already acquired for container test and zip file 1_24-06-2018-00-00-00.zip")
        ).isEqualTo(4);

        // We expect only one envelope which was uploaded and other failed
        List<Envelope> envelopes = envelopeRepository.findAll();
        assertThat(envelopes.size()).isEqualTo(1);

        // and
        // We expect only two events one for doc upload and another for doc processed
        List<ProcessEvent> processEvents = processEventRepository.findAll();
        assertThat(processEvents).hasSize(2);
    }

    @NotNull
    private List<Future<Void>> processBlobUsingExecutor(int numberOfThreads) throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(
            numberOfThreads,
            ThreadFactories.withName("BSP-ACQUIRE-LEASE-TEST")
        );

        List<Callable<Void>> tasks = new ArrayList<>();

        for (int threadCount = 0; threadCount < numberOfThreads; threadCount++) {
            tasks.add(callableTask());
        }

        return executorService.invokeAll(tasks);
    }

    private Callable<Void> callableTask() {
        return () -> {
            processor.processBlobs();
            return null;
        };
    }

    private void givenValidZipFileUploadedAndDocStoreMocked() throws Exception {
        uploadZipToBlobStore(ZIP_FILE_NAME_SUCCESS);

        given(documentManagementService.uploadDocuments(
            ImmutableList.of(
                new Pdf("1111001.pdf", toByteArray(getResource("1111001.pdf"))),
                new Pdf("1111002.pdf", toByteArray(getResource("1111002.pdf")))
            ))).willReturn(getFileUploadResponse());
    }
}
