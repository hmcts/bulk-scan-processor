package uk.gov.hmcts.reform.bulkscanprocessor.services.reports;

import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.ListBlobItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.RejectedFile;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.BlobManager;

import java.net.URI;
import java.util.EnumSet;
import java.util.List;

import static com.microsoft.azure.storage.blob.BlobListingDetails.SNAPSHOTS;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class RejectedFilesReportServiceTest {

    @Mock private BlobManager blobManager;
    @Mock private CloudBlobContainer containerA;
    @Mock private CloudBlobContainer containerB;

    private RejectedFilesReportService service;

    @BeforeEach
    public void setUp() throws Exception {
        given(blobManager.listRejectedContainers())
            .willReturn(asList(containerA, containerB));

        service = new RejectedFilesReportService(blobManager);
    }

    @Test
    public void should_return_empty_list_when_there_are_no_rejected_files() {
        // given
        setUpContainer(containerA, emptyList());
        setUpContainer(containerB, emptyList());

        // when
        List<RejectedFile> result = service.getRejectedFiles();

        // then
        assertThat(result).isEmpty();
    }

    @Test
    public void should_get_files_from_rejected_containers() {
        // given
        given(containerA.getName()).willReturn("A");
        given(containerB.getName()).willReturn("B");

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

    private ListBlobItem mockItem(String filename) {
        ListBlobItem item = mock(ListBlobItem.class);
        given(item.getUri()).willReturn(URI.create(filename));
        return item;
    }

    private void setUpContainer(CloudBlobContainer container, List<ListBlobItem> listBlobItems) {
        given(container.listBlobs(null, true, EnumSet.of(SNAPSHOTS), null, null))
            .willReturn(listBlobItems);
    }
}
