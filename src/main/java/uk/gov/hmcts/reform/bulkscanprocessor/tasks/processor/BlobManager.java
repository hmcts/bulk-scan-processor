package uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor;

import com.azure.core.util.Context;
import com.azure.core.util.polling.PollResponse;
import com.azure.core.util.polling.SyncPoller;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobContainerItem;
import com.azure.storage.blob.models.BlobCopyInfo;
import com.azure.storage.blob.models.BlobErrorCode;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.models.DeleteSnapshotsOptionType;
import com.azure.storage.blob.sas.BlobContainerSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.config.BlobManagementProperties;

import java.net.HttpURLConnection;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.toList;

@Component
@EnableConfigurationProperties(BlobManagementProperties.class)
public class BlobManager {

    private static final Logger log = LoggerFactory.getLogger(BlobManager.class);
    private static final String REJECTED_CONTAINER_NAME_SUFFIX = "-rejected";
    private static final String SELECT_ALL_CONTAINER = "ALL";
    public static final Map<String, String> META_DATA_MAP = Map.of("waitingCopy", "true");

    private final BlobServiceClient blobServiceClient;

    private final BlobManagementProperties properties;

    public BlobManager(
        BlobServiceClient blobServiceClient,
        BlobManagementProperties properties
    ) {
        this.blobServiceClient = blobServiceClient;
        this.properties = properties;
    }

    public BlobContainerClient listContainerClient(String containerName) {
        return blobServiceClient.getBlobContainerClient(containerName);
    }

    public List<BlobContainerClient> listInputContainerClients() {
        List<BlobContainerClient> blobContainerClientList =
            blobServiceClient
                .listBlobContainers()
                .stream()
                .filter(c -> !c.getName().endsWith(REJECTED_CONTAINER_NAME_SUFFIX))
                .filter(this::filterBySelectedContainer)
                .map(c -> blobServiceClient.getBlobContainerClient(c.getName()))
                .collect(toList());
        if (blobContainerClientList.isEmpty()) {
            log.error("Container not found for configured container name : {}", properties.getBlobSelectedContainer());
        }
        return blobContainerClientList;
    }

    private boolean filterBySelectedContainer(BlobContainerItem container) {
        String selectedContainer = properties.getBlobSelectedContainer();
        return SELECT_ALL_CONTAINER.equalsIgnoreCase(selectedContainer)
            || selectedContainer.equals(container.getName());
    }

    public List<BlobContainerClient> listRejectedContainers() {
        return blobServiceClient.listBlobContainers()
            .stream()
            .filter(c -> c.getName().endsWith(REJECTED_CONTAINER_NAME_SUFFIX))
            .map(c -> blobServiceClient.getBlobContainerClient(c.getName()))
            .collect(toList());
    }

    public void tryMoveFileToRejectedContainer(String fileName, String inputContainerName, String leaseId) {
        String rejectedContainerName = getRejectedContainerName(inputContainerName);

        try {
            moveFileToRejectedContainer(
                fileName,
                inputContainerName,
                rejectedContainerName,
                leaseId
            );
        } catch (Exception ex) {
            log.error(
                "An error occurred when moving rejected file {} from container {} to rejected files' container {}",
                fileName,
                inputContainerName,
                rejectedContainerName,
                ex
            );
        }
    }

    private void moveFileToRejectedContainer(
        String fileName,
        String inputContainerName,
        String rejectedContainerName,
        String leaseId
    ) {
        log.info("Moving file {} from container {} to {}", fileName, inputContainerName, rejectedContainerName);
        BlobClient inputBlob = blobServiceClient
            .getBlobContainerClient(inputContainerName)
            .getBlobClient(fileName);

        BlobClient rejectedBlob = blobServiceClient
            .getBlobContainerClient(rejectedContainerName)
            .getBlobClient(fileName);

        if (rejectedBlob.exists()) {
            // next steps will overwrite the file, create a snapshot of current version
            rejectedBlob.createSnapshot();
        }

        copyToRejectedContainer(inputBlob, rejectedBlob);

        log.info("Rejected file copied to rejected container: {} ", rejectedContainerName);

        try {
            inputBlob.deleteWithResponse(
                DeleteSnapshotsOptionType.INCLUDE,
                null,
                null,
                Context.NONE
            );
        } catch (BlobStorageException e) {
            //if lease lost retry
            log.warn(
                "Deleting File {} got error, Error code {}, Http status {} ",
                fileName,
                e.getErrorCode(),
                e.getStatusCode(),
                e
            );

            if (e.getStatusCode() == HttpURLConnection.HTTP_PRECON_FAILED
                && BlobErrorCode.LEASE_LOST.equals(e.getErrorCode())) {
                log.info("Deleting File {} got error, retrying...", fileName);
                inputBlob.deleteWithResponse(
                    DeleteSnapshotsOptionType.ONLY,
                    null,
                    null,
                    Context.NONE
                );
            } else {
                throw e;
            }
        }
        log.info("File {} moved to rejected container {}", fileName, rejectedContainerName);
    }

    private String getRejectedContainerName(String inputContainerName) {
        return inputContainerName + REJECTED_CONTAINER_NAME_SUFFIX;
    }

    private void copyToRejectedContainer(BlobClient sourceBlob, BlobClient targetBlob) {
        String sasToken = sourceBlob
            .generateSas(
                new BlobServiceSasSignatureValues(
                    OffsetDateTime
                        .of(LocalDateTime.now().plus(5, ChronoUnit.MINUTES), ZoneOffset.UTC),
                    new BlobContainerSasPermission().setReadPermission(true)
                )
            );

        var start = System.nanoTime();
        SyncPoller<BlobCopyInfo, Void> poller = null;
        try {
            poller = targetBlob
                .beginCopy(
                    sourceBlob.getBlobUrl() + "?" + sasToken,
                    META_DATA_MAP,
                    null,
                    null,
                    null,
                    null,
                    Duration.ofSeconds(2)
                );

            PollResponse<BlobCopyInfo> pollResponse = poller
                .waitForCompletion(Duration.ofMinutes(5));
            targetBlob.setMetadata(null);
            log.info("Moved to rejected container from {}. Poll response: {}, Copy status: {} ,Takes {} second",
                sourceBlob.getBlobUrl(),
                pollResponse.getStatus(),
                pollResponse.getValue().getCopyStatus(),
                TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start)
            );

        } catch (Exception ex) {
            log.error("Copy Error, for {} to rejected container",
                sourceBlob.getBlobUrl(),
                ex
            );

            if (poller != null) {
                try {
                    targetBlob.abortCopyFromUrl(poller.poll().getValue().getCopyId());
                } catch (Exception exc) {
                    log.error("Abort Copy From Url got Error, From {} to rejected container",
                        sourceBlob.getBlobUrl(),
                        exc
                    );
                }
            }
            throw ex;
        }
    }

}
