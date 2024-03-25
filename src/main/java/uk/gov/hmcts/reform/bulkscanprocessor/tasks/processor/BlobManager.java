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

/**
 * Manages the blob storage.
 */
@Component
@EnableConfigurationProperties(BlobManagementProperties.class)
public class BlobManager {

    private static final Logger log = LoggerFactory.getLogger(BlobManager.class);
    private static final String REJECTED_CONTAINER_NAME_SUFFIX = "-rejected";
    private static final String SELECT_ALL_CONTAINER = "ALL";
    public static final Map<String, String> META_DATA_MAP = Map.of("waitingCopy", "true");

    private final BlobServiceClient blobServiceClient;

    private final BlobManagementProperties properties;

    /**
     * Constructor for the BlobManager.
     * @param blobServiceClient The blob service client
     * @param properties The blob management properties
     */
    public BlobManager(
        BlobServiceClient blobServiceClient,
        BlobManagementProperties properties
    ) {
        this.blobServiceClient = blobServiceClient;
        this.properties = properties;
    }

    /**
     * Lists the container client.
     * @param containerName The container name
     * @return The blob container client
     */
    public BlobContainerClient listContainerClient(String containerName) {
        return blobServiceClient.getBlobContainerClient(containerName);
    }

    /**
     * Lists the input container clients.
     * @return The blob container clients
     */
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

    /**
     * Filters the selected container.
     * @param container The blob container item
     * @return The boolean value
     */
    private boolean filterBySelectedContainer(BlobContainerItem container) {
        String selectedContainer = properties.getBlobSelectedContainer();
        return SELECT_ALL_CONTAINER.equalsIgnoreCase(selectedContainer)
            || selectedContainer.equals(container.getName());
    }

    /**
     * Lists the rejected containers.
     * @return The blob container clients
     */
    public List<BlobContainerClient> listRejectedContainers() {
        return blobServiceClient.listBlobContainers()
            .stream()
            .filter(c -> c.getName().endsWith(REJECTED_CONTAINER_NAME_SUFFIX))
            .map(c -> blobServiceClient.getBlobContainerClient(c.getName()))
            .collect(toList());
    }

    /**
     * Tries to move the file to the rejected container.
     * @param fileName The file name
     * @param inputContainerName The input container name
     * @throws Exception If an error occurs
     */
    public void tryMoveFileToRejectedContainer(String fileName, String inputContainerName) {
        String rejectedContainerName = getRejectedContainerName(inputContainerName);

        try {
            moveFileToRejectedContainer(
                fileName,
                inputContainerName,
                rejectedContainerName
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

    /**
     * Moves the file to the rejected container.
     * @param fileName The file name
     * @param inputContainerName The input container name
     * @param rejectedContainerName The rejected container name
     * @throws BlobStorageException If an error occurs
     */
    private void moveFileToRejectedContainer(
        String fileName,
        String inputContainerName,
        String rejectedContainerName
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

    /**
     * Gets the rejected container name.
     * @param inputContainerName The input container name
     * @return The rejected container name
     */
    private String getRejectedContainerName(String inputContainerName) {
        return inputContainerName + REJECTED_CONTAINER_NAME_SUFFIX;
    }

    /**
     * Copies the blob to the rejected container.
     * @param sourceBlob The source blob
     * @param targetBlob The target blob
     */
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
