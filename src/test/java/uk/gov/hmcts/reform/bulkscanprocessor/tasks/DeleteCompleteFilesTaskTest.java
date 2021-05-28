package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.azure.storage.blob.BlobContainerClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.services.DeleteFilesService;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.BlobManager;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"PMD"})
class DeleteCompleteFilesTaskTest {

    @Mock
    private BlobManager blobManager;

    @Mock
    private DeleteFilesService deleteFilesService;

    @Mock
    private BlobContainerClient container1;

    private DeleteCompleteFilesTask deleteCompleteFilesTask;

    @BeforeEach
    void setUp() {
        deleteCompleteFilesTask = new DeleteCompleteFilesTask(
            blobManager,
            deleteFilesService
        );
    }

    @Test
    void should_process_single_container() {
        // given
        given(blobManager.listInputContainerClients()).willReturn(singletonList(container1));

        // when
        deleteCompleteFilesTask.run();

        // then
        verifyNoMoreInteractions(blobManager);
    }

    @Test
    void should_process_multiple_containers() {
        // given
        final BlobContainerClient container2 = mock(BlobContainerClient.class);

        given(blobManager.listInputContainerClients()).willReturn(asList(container1, container2));

        // when
        deleteCompleteFilesTask.run();

        // then
        verify(deleteFilesService).processCompleteFiles(container1);
        verify(deleteFilesService).processCompleteFiles(container2);
        verifyNoMoreInteractions(blobManager);
    }

    @Test
    void should_process_second_container_if_processing_the_first_container_throws() {
        // given
        final BlobContainerClient container2 = mock(BlobContainerClient.class);

        given(blobManager.listInputContainerClients()).willReturn(asList(container1, container2));
        doThrow(new RuntimeException("error message")).when(deleteFilesService).processCompleteFiles(container1);

        // then
        deleteCompleteFilesTask.run();

        // then
        verify(deleteFilesService).processCompleteFiles(container1);
        verify(deleteFilesService).processCompleteFiles(container2);
        verifyNoMoreInteractions(blobManager);
    }
}
