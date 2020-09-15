package uk.gov.hmcts.reform.bulkscanprocessor.services.storage;

import com.azure.core.util.Context;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.models.BlobRequestConditions;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.time.LocalDateTime.now;
import static java.time.LocalDateTime.parse;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.reform.bulkscanprocessor.util.TimeZones.EUROPE_LONDON_ZONE_ID;

@Component
public class OcrValidationRetryManager {

    private static final Logger logger = getLogger(OcrValidationRetryManager.class);

    private static final String RETRY_COUNT_METADATA_PROPERTY = "ocrValidationRetryCount";
    private static final String RETRY_DELAY_EXPIRATION_TIME_METADATA_PROPERTY =
        "ocrValidationRetryDelayExpirationTime";

    private final LeaseAcquirer leaseAcquirer;
    private final int ocrValidationMaxRetries;
    private final int ocrValidationRetryDelaySec;

    public OcrValidationRetryManager(
        LeaseAcquirer leaseAcquirer,
        @Value("${ocr-validation-max-retries}") int ocrValidationMaxRetries,
        @Value("${ocr-validation-delay-retry-sec}") int ocrValidationRetryDelaySec
    ) {
        this.leaseAcquirer = leaseAcquirer;
        this.ocrValidationMaxRetries = ocrValidationMaxRetries;
        this.ocrValidationRetryDelaySec = ocrValidationRetryDelaySec;
    }

    public boolean isReadyToRetry(BlobClient blobClient) {
        Map<String, String> blobMetaData = blobClient.getProperties().getMetadata();
        String retryDelayExpirationTime = blobMetaData.get(RETRY_DELAY_EXPIRATION_TIME_METADATA_PROPERTY);

        final boolean retryDelayExpired = isRetryDelayExpired(retryDelayExpirationTime);

        if (!retryDelayExpired) {
            logger.info(
                "Retry delay not yet expired on file {} in container {}",
                blobClient.getBlobName(),
                blobClient.getContainerName()
            );
        }

        return retryDelayExpired;
    }

    /**
     * Check if number of retries is exceeding ocrValidationMaxRetries,
     * if not set new retry delay expiration time, increment retryCount and return true,
     * otherwise return false which means no more retries are possible.
     *
     * @return true if retry is possible and new retry delay expiration time has been set,
     *         false if no more retries are possible
     */
    public boolean setRetryDelayIfPossible(BlobClient blobClient) {
        final AtomicBoolean res = new AtomicBoolean();

        leaseAcquirer.ifAcquiredOrElse(
            blobClient,
            leaseId -> trySetRetryDelayIfPossible(blobClient, leaseId, res),
            blobErrorCode -> {
            },
            true
        );

        return res.get();
    }

    private void trySetRetryDelayIfPossible(BlobClient blobClient, String leaseId, AtomicBoolean res) {
        Map<String, String> blobMetaData = blobClient.getProperties().getMetadata();
        int retryCount = getRetryCount(blobMetaData);

        if (retryCount > ocrValidationMaxRetries) {
            res.set(false);
        } else {
            prepareNextRetry(blobClient, blobMetaData, retryCount, leaseId);
            res.set(true);
        }
    }

    private void prepareNextRetry(
        BlobClient blobClient,
        Map<String, String> blobMetaData,
        int retryCount,
        String leaseId
    ) {
        blobMetaData.put(RETRY_COUNT_METADATA_PROPERTY, Integer.toString(retryCount + 1));
        blobMetaData.put(
            RETRY_DELAY_EXPIRATION_TIME_METADATA_PROPERTY,
            now(EUROPE_LONDON_ZONE_ID).plusSeconds(ocrValidationRetryDelaySec).toString()
        );
        blobClient.setMetadataWithResponse(
            blobMetaData,
            new BlobRequestConditions().setLeaseId(leaseId),
            null,
            Context.NONE
        );
    }

    private boolean isRetryDelayExpired(String retryDelayExpirationTime) {
        if (StringUtils.isBlank(retryDelayExpirationTime)) {
            return true; // no retry so far
        } else {
            LocalDateTime retryDelayExpiresAt = parse(retryDelayExpirationTime);
            return retryDelayExpiresAt.isBefore(now(EUROPE_LONDON_ZONE_ID)); // check if delay expired
        }
    }

    private int getRetryCount(Map<String, String> blobMetaData) {
        String retryCountStr = blobMetaData.get(RETRY_COUNT_METADATA_PROPERTY);

        if (StringUtils.isBlank(retryCountStr)) {
            return 0;
        } else {
            return Integer.parseInt(retryCountStr);
        }
    }
}
