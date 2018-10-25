package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.google.common.collect.ImmutableMap;
import com.netflix.servo.util.ThreadFactories;
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
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Event;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.DirectoryZipper.zipDir;

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
        uploadToBlobStorage(SAMPLE_ZIP_FILE_NAME, zipDir("zipcontents/ok"));

        given(documentManagementService.uploadDocuments(any()))
            .willReturn(ImmutableMap.of(
                "1111002.pdf", DOCUMENT_URL2
            ));

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

        // Only one thread should be able to acquire lease and others should fail.
        // Therefore we expect only one envelope which was uploaded and others failed
        List<Envelope> envelopes = envelopeRepository.findAll();
        assertThat(envelopes.size()).isEqualTo(1);

        // and
        List<ProcessEvent> processEvents = processEventRepository.findAll();
        assertThat(processEvents)
            .extracting("event")
            .containsExactlyInAnyOrder(
                Event.DOC_UPLOADED, Event.DOC_PROCESSED, Event.DOC_PROCESSED_NOTIFICATION_SENT
            );
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

}
