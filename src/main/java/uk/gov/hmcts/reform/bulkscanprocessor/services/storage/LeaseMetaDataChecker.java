package uk.gov.hmcts.reform.bulkscanprocessor.services.storage;

import com.azure.core.util.Context;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.models.BlobRequestConditions;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.config.BlobManagementProperties;

import java.time.LocalDateTime;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.reform.bulkscanprocessor.util.TimeZones.EUROPE_LONDON_ZONE_ID;

@Component
public class LeaseMetaDataChecker {

    private static final Logger logger = getLogger(LeaseAcquirer.class);

    public static final String LEASE_EXPIRATION_TIME = "leaseExpirationTime";

    private final BlobManagementProperties properties;

    public LeaseMetaDataChecker(BlobManagementProperties properties) {
        this.properties = properties;
    }

    public boolean isReadyToUse(BlobClient blobClient) {
        var blobProperties = blobClient.getProperties();
        Map<String, String> blobMetaData = blobProperties.getMetadata();
        String etag = blobProperties.getETag();

        String leaseExpirationTime = blobMetaData.get(LEASE_EXPIRATION_TIME);
        var zipFilename = blobClient.getBlobName();
        var containerName = blobClient.getContainerName();

        logger.info(
            "Checking if lease acquired on file {} in container {}. Lease Expiration Time: {}",
            zipFilename,
            containerName,
            leaseExpirationTime
        );

        if (isMetaDataLeaseExpired(leaseExpirationTime)) {
            blobMetaData.put(
                LEASE_EXPIRATION_TIME,
                LocalDateTime.now(EUROPE_LONDON_ZONE_ID)
                    .plusSeconds(this.properties.getBlobLeaseAcquireDelayInSeconds()).toString()
            );
            blobClient.setMetadataWithResponse(
                blobMetaData,
                new BlobRequestConditions().setIfMatch("\"" + etag + "\""),
                null,
                Context.NONE
            );
            return true;
        } else {
            logger.info(
                "Lease already acquired on file {} in container {} less than {} seconds ago. ",
                zipFilename,
                containerName,
                blobMetaData.get(LEASE_EXPIRATION_TIME)
            );
            return false;
        }
    }

    private boolean isMetaDataLeaseExpired(String leaseExpirationTime) {
        if (StringUtils.isBlank(leaseExpirationTime)) {
            return true; // lease not acquired on file
        } else {
            LocalDateTime leaseExpiresAt = LocalDateTime.parse(leaseExpirationTime);
            return leaseExpiresAt
                .isBefore(LocalDateTime.now(EUROPE_LONDON_ZONE_ID)); // check if lease expired
        }
    }

    public void clearMetaData(BlobClient blobClient) {
        Map<String, String> blobMetaData = blobClient.getProperties().getMetadata();
        blobMetaData.remove(LEASE_EXPIRATION_TIME);
        blobClient.setMetadata(blobMetaData);
    }
}
