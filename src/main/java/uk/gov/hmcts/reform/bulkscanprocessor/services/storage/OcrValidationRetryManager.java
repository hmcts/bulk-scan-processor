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

import static java.time.LocalDateTime.now;
import static java.time.LocalDateTime.parse;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.reform.bulkscanprocessor.util.TimeZones.EUROPE_LONDON_ZONE_ID;

/**
 * Manages the retry mechanism for OCR validation.
 */
@Component
public class OcrValidationRetryManager {

    private static final Logger logger = getLogger(OcrValidationRetryManager.class);

    private static final String RETRY_COUNT_METADATA_PROPERTY = "ocrValidationRetryCount";
    private static final String RETRY_DELAY_EXPIRATION_TIME_METADATA_PROPERTY =
        "ocrValidationRetryDelayExpirationTime";

    private final int ocrValidationMaxRetries;
    private final int ocrValidationRetryDelaySec;

    /**
     * Constructor for OcrValidationRetryManager.
     * @param ocrValidationMaxRetries The maximum number of retries
     * @param ocrValidationRetryDelaySec The delay between retries
     */
    public OcrValidationRetryManager(
        @Value("${ocr-validation-max-retries}") int ocrValidationMaxRetries,
        @Value("${ocr-validation-delay-retry-sec}") int ocrValidationRetryDelaySec
    ) {
        this.ocrValidationMaxRetries = ocrValidationMaxRetries;
        this.ocrValidationRetryDelaySec = ocrValidationRetryDelaySec;
    }

    /**
     * Checks if the retry delay has expired and can process the file.
     * @param blobClient The blob client
     * @return true if the retry delay has expired, false otherwise
     */
    public boolean canProcess(BlobClient blobClient) {
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
     * <p>Check if number of retries is exceeding ocrValidationMaxRetries,
     * if not set new retry delay expiration time, increment retryCount and return true,
     * otherwise return false which means no more retries are possible.</p>
     *
     * <p>The lease of the file MUST be already acquired and leaseId should be provided
     * as the value of the second parameter.</p>
     *
     * @return true if retry is possible and new retry delay expiration time has been set,
     *         false if no more retries are possible
     */
    public boolean setRetryDelayIfPossible(BlobClient blobClient, String leaseId) {
        Map<String, String> blobMetaData = blobClient.getProperties().getMetadata();
        int retryCount = getRetryCount(blobMetaData);

        if (retryCount > ocrValidationMaxRetries) {
            return false;
        } else {
            prepareNextRetry(blobClient, blobMetaData, retryCount, leaseId);
            return true;
        }
    }

    /**
     * Resets the retry count and delay expiration time.
     * @param blobClient The blob client
     * @param leaseId The lease id
     * @param blobMetaData The blob metadata
     * @param retryCount The retry count
     */
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

    /**
     * Checks if the retry delay has expired.
     * @param retryDelayExpirationTime The retry delay expiration time
     * @return true if the retry delay has expired, false otherwise
     */
    private boolean isRetryDelayExpired(String retryDelayExpirationTime) {
        if (StringUtils.isBlank(retryDelayExpirationTime)) {
            return true; // no retry so far
        } else {
            LocalDateTime retryDelayExpiresAt = parse(retryDelayExpirationTime);
            return retryDelayExpiresAt.isBefore(now(EUROPE_LONDON_ZONE_ID)); // check if delay expired
        }
    }

    /**
     * Gets the retry count from the blob metadata.
     * @param blobMetaData The blob metadata
     * @return The retry count
     */
    private int getRetryCount(Map<String, String> blobMetaData) {
        String retryCountStr = blobMetaData.get(RETRY_COUNT_METADATA_PROPERTY);

        if (StringUtils.isBlank(retryCountStr)) {
            return 0;
        } else {
            return Integer.parseInt(retryCountStr);
        }
    }
}
