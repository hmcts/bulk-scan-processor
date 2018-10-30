package uk.gov.hmcts.reform.bulkscanprocessor.util;

import com.microsoft.azure.storage.blob.BlobAccessConditions;
import com.microsoft.azure.storage.blob.BlockBlobURL;
import com.microsoft.azure.storage.blob.ContainerURL;
import com.microsoft.azure.storage.blob.ListBlobsOptions;
import com.microsoft.azure.storage.blob.ListContainersOptions;
import com.microsoft.azure.storage.blob.ReliableDownloadOptions;
import com.microsoft.azure.storage.blob.ServiceURL;
import com.microsoft.azure.storage.blob.models.BlobFlatListSegment;
import com.microsoft.azure.storage.blob.models.BlobItem;
import com.microsoft.azure.storage.blob.models.ContainerItem;
import com.microsoft.azure.storage.blob.models.ContainerListBlobFlatSegmentResponse;
import com.microsoft.azure.storage.blob.models.LeaseAccessConditions;
import com.microsoft.azure.storage.blob.models.LeaseStateType;
import com.microsoft.azure.storage.blob.models.ServiceListContainersSegmentResponse;
import com.microsoft.rest.v2.Context;
import com.microsoft.rest.v2.util.FlowableUtil;
import io.reactivex.Observable;
import io.reactivex.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;

@Component
public class AzureStorageHelper {

    private final ServiceURL client;
    private static final Logger LOGGER = LoggerFactory.getLogger(AzureStorageHelper.class);

    @Autowired
    public AzureStorageHelper(ServiceURL client) {
        this.client = client;
    }

    public ServiceURL getClient() {
        return client;
    }

    public Single<ServiceListContainersSegmentResponse> listContainers() {
        return client.listContainersSegment(null, ListContainersOptions.DEFAULT, null)
            .flatMap(this::listContainers);
    }

    public Observable<BlobItem> listBlobsLazy(ContainerURL containerURL) {
        return containerURL.listBlobsFlatSegment(null, null, null)
            .flatMapObservable((r) -> listContainersResultToContainerObservable(containerURL, null, r));
    }

    private static Observable<BlobItem> listContainersResultToContainerObservable(
        ContainerURL containerURL, ListBlobsOptions listBlobsOptions,
        ContainerListBlobFlatSegmentResponse response
    ) {
        BlobFlatListSegment segment = response.body().segment();
        if (segment != null) {
            Observable<BlobItem> result = Observable.fromIterable(segment.blobItems());

            LOGGER.info("Count: {}", segment.blobItems());

            if (response.body().nextMarker() != null) {
                LOGGER.info("Hit continuation in listing at {}", segment.blobItems().get(
                    segment.blobItems().size() - 1).name());
                // Recursively add the continuation items to the observable.
                result = result
                    .concatWith(containerURL.listBlobsFlatSegment(response.body().nextMarker(), listBlobsOptions, null)
                        .flatMapObservable((r) ->
                            listContainersResultToContainerObservable(containerURL, listBlobsOptions, r)));
            }

            return result;
        }

        return Observable.empty();
    }

    private Single<ServiceListContainersSegmentResponse> listContainers(
        ServiceListContainersSegmentResponse response
    ) {

        // Process the containers returned in this result segment (if the segment is empty, containerItems will be null.
        if (response.body().containerItems() != null) {
            for (ContainerItem b : response.body().containerItems()) {
                String output = "Container name: " + b.name();
                System.out.println(output);
            }
        }

        // If there is not another segment, return this response as the final response.
        if (response.body().nextMarker() == null) {
            return Single.just(response);
        } else {
            /*
             IMPORTANT: ListContainersSegment returns the start of the next segment; you MUST use this to get the
             next segment (after processing the current result segment
             */
            String nextMarker = response.body().nextMarker();

            /*
            The presence of the marker indicates that there are more blobs to list, so we make another call to
            listContainersSegment and pass the result through this helper function.
             */
            return client.listContainersSegment(nextMarker, ListContainersOptions.DEFAULT, null)
                .flatMap(containersListBlobHierarchySegmentResponse -> listContainers(response));
        }
    }

    public boolean checkLeaseAvailable(
        String containerName,
        String zipFilename,
        LeaseStateType leaseStateType
    ) {
        if (leaseStateType == LeaseStateType.LEASED) {
            LOGGER.debug("Lease already acquired for container {} and zip file {}",
                containerName, zipFilename);
            return false;
        }
        return true;
    }

    public Single<ByteBuffer> downloadBlob(BlockBlobURL blockBlobURL) {
        return blockBlobURL.acquireLease(null, -1, null, Context.NONE)
            .flatMap(response -> {
                String leaseId = response.headers().leaseId();
                BlobAccessConditions accessConditions = new BlobAccessConditions()
                    .withLeaseAccessConditions(new LeaseAccessConditions().withLeaseId(leaseId));

                return blockBlobURL.download(null, accessConditions, false, Context.NONE);
            })
            .flatMap(response -> FlowableUtil
                .collectBytesInBuffer(response.body(new ReliableDownloadOptions().withMaxRetryRequests(5))));
    }
}
