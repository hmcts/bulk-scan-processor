package uk.gov.hmcts.reform.bulkscanprocessor.services.reports;

import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.ListBlobItem;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.RejectedFile;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.BlobManager;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.microsoft.azure.storage.blob.BlobListingDetails.SNAPSHOTS;
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
                        FilenameUtils.getName(listItem.getUri().toString()),
                        container.getName()
                    )))
            .collect(toList());
    }

    private Stream<ListBlobItem> getBlobs(CloudBlobContainer container) {
        return StreamSupport.stream(
            container
                .listBlobs(null, true, EnumSet.of(SNAPSHOTS), null, null)
                .spliterator(),
            false
        );
    }
}
