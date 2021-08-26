package uk.gov.hmcts.reform.bulkscanprocessor.services.reports;

import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.RejectedFile;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.BlobManager;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class RejectedFilesReportServiceTest {

    @Mock private BlobManager blobManager;
    @Mock private BlobContainerClient containerA;
    @Mock private BlobContainerClient containerB;

    private RejectedFilesReportService service;

    @BeforeEach
    void setUp() throws Exception {
        given(blobManager.listRejectedContainers())
            .willReturn(asList(containerA, containerB));

        service = new RejectedFilesReportService(blobManager);
    }

    @Test
    void should_return_empty_list_when_there_are_no_rejected_files() {
        // given
        setUpContainer(containerA, emptyList());
        setUpContainer(containerB, emptyList());

        // when
        List<RejectedFile> result = service.getRejectedFiles();

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void should_get_files_from_rejected_containers() {
        // given
        given(containerA.getBlobContainerName()).willReturn("A");
        given(containerB.getBlobContainerName()).willReturn("B");

        setUpContainer(containerA, asList(mockItem("a1.zip"), mockItem("a2.zip")));
        setUpContainer(containerB, asList(mockItem("b1.zip"), mockItem("b2.zip"), mockItem("b3.zip")));

        // when
        List<RejectedFile> result = service.getRejectedFiles();

        // then
        assertThat(result)
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new RejectedFile("a1.zip", "A"),
                new RejectedFile("a2.zip", "A"),
                new RejectedFile("b1.zip", "B"),
                new RejectedFile("b2.zip", "B"),
                new RejectedFile("b3.zip", "B")
            );
    }

    private BlobItem mockItem(String filename) {
        BlobItem item = mock(BlobItem.class);
        given(item.getName()).willReturn(filename);
        return item;
    }

    @SuppressWarnings("unchecked")
    private void setUpContainer(BlobContainerClient container, List<BlobItem> listBlobItems) {
        PagedIterable pagedIterable = mock(PagedIterable.class);
        given(pagedIterable.stream())
            .willReturn(listBlobItems.stream());

        given(container.listBlobs(any(), eq(null)))
            .willReturn(pagedIterable);
    }
}
