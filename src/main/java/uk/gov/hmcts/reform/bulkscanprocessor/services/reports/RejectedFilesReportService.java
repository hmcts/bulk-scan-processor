package uk.gov.hmcts.reform.bulkscanprocessor.services.reports;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.ListBlobsOptions;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.RejectedFile;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.BlobManager;

import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@Service
public class RejectedFilesReportService {

    private final BlobManager blobManager;

    public RejectedFilesReportService(BlobManager blobManager) {
        this.blobManager = blobManager;
    }

    public List<RejectedFile> getRejectedFiles() {
        return blobManager
            .listRejectedContainers()
            .stream()
            .flatMap(container ->
                getBlobs(container)
                    .map(listItem -> new RejectedFile(
                        listItem.getName(),
                        container.getBlobContainerName()
                    )))
            .collect(toList());
    }

    private Stream<BlobItem> getBlobs(BlobContainerClient container) {
        ListBlobsOptions listOptions = new ListBlobsOptions();
        listOptions.getDetails().setRetrieveSnapshots(true);
        return container
                .listBlobs(listOptions, null).stream();
    }
}
