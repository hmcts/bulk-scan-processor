package uk.gov.hmcts.reform.bulkscanprocessor.services.storage;

import com.azure.storage.blob.BlobClient;
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

@Component
public class OcrValidationRetryManager {

    private static final Logger logger = getLogger(OcrValidationRetryManager.class);

    private static final String RETRY_COUNT = "ocrValidationRetryCount";
    private static final String RETRY_DELAY_EXPIRATION_TIME = "ocrValidationRetryDelayExpirationTime";

    private final int ocrValidationMaxRetries;
    private final int ocrValidationRetryDelaySec;

    public OcrValidationRetryManager(
        @Value("${ocr-validation-max-retries}") int ocrValidationMaxRetries,
        @Value("${ocr-validation-delay-retry-sec}") int ocrValidationRetryDelaySec
    ) {
        this.ocrValidationMaxRetries = ocrValidationMaxRetries;
        this.ocrValidationRetryDelaySec = ocrValidationRetryDelaySec;
    }

    public boolean isReadyToRetry(BlobClient blobClient) {
        Map<String, String> blobMetaData = blobClient.getProperties().getMetadata();
        String retryDelayExpirationTime = blobMetaData.get(RETRY_DELAY_EXPIRATION_TIME);
        var zipFilename = blobClient.getBlobName();
        var containerName = blobClient.getContainerName();

        logger.info(
            "Checking if retry delay expired on file {} in container {}",
            zipFilename,
            containerName
        );

        return isRetryDelayExpired(retryDelayExpirationTime);
    }

    /**
     * Check if number of retries is exceeding ocrValidationMaxRetries,
     * if not set new retry delay expiration time, increment retryCount and return true,
     * otherwise return false which means no more retries are possible
     *
     * @return true if retry is possible and new retry delay expiration time has been set,
     * false if no more retries are possible
     */
    public boolean setRetryDelayIfPossible(BlobClient blobClient) {
        Map<String, String> blobMetaData = blobClient.getProperties().getMetadata();
        int retryCount = getRetryCount(blobMetaData);

        if (retryCount >= ocrValidationMaxRetries) {
            return false;
        } else {
            blobMetaData.put(RETRY_COUNT, Integer.toString(retryCount + 1));
            blobMetaData.put(
                RETRY_DELAY_EXPIRATION_TIME,
                now(EUROPE_LONDON_ZONE_ID).plusSeconds(ocrValidationRetryDelaySec).toString()
            );
            blobClient.setMetadata(blobMetaData);
            return true;
        }
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
        String retryCountStr = blobMetaData.get(RETRY_COUNT);

        if (StringUtils.isBlank(retryCountStr)) {
            return 0;
        } else {
            return Integer.parseInt(retryCountStr);
        }
    }
}
