package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.bulkscanprocessor.config.IntegrationTest;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.DirectoryZipper.zipDir;

@IntegrationTest
public class BlobProcessorTaskTestWithAcquireLease extends ProcessorTestSuite {

    @Test
    public void should_process_zip_file_only_once_by_processor_thread_which_acquires_lease_on_blob()
        throws Exception {

        // Given
        uploadToBlobStorage(SAMPLE_ZIP_FILE_NAME, zipDir("zipcontents/ok"));

        given(documentManagementService.uploadDocuments(any(), eq("BULKSCAN"), eq("bulkscan")))
            .willReturn(ImmutableMap.of(
                "1111002.pdf", DOCUMENT_URL2
            ));

        // When
        List<Future<Void>> futureTasks = processBlobUsingExecutor();

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
            .extracting(ProcessEvent::getEvent)
            .containsExactlyInAnyOrder(Event.ZIPFILE_PROCESSING_STARTED);
    }

    private List<Future<Void>> processBlobUsingExecutor() {
        List<Future<Void>> tasks = new ArrayList<>();

        for (int threadCount = 0; threadCount < 5; threadCount++) {
            tasks.add(CompletableFuture.runAsync(runnableTask()));
        }

        return tasks;
    }

    private Runnable runnableTask() {
        return () -> {
            try {
                processor.processBlobs();
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        };
    }

}
