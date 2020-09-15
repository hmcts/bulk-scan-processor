package uk.gov.hmcts.reform.bulkscanprocessor.services.storage;

import com.azure.core.util.Context;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.models.BlobRequestConditions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static java.time.LocalDateTime.now;
import static java.time.LocalDateTime.parse;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.bulkscanprocessor.util.TimeZones.EUROPE_LONDON_ZONE_ID;

@ExtendWith(MockitoExtension.class)
class OcrValidationRetryManagerTest {
    private static final int OCR_VALIDATION_MAX_RETRIES = 2;
    private static final int OCR_VALIDATION_RETRY_DELAY_SEC = 600;

    private OcrValidationRetryManager ocrValidationRetryManager;

    @Mock
    private BlobClient blobClient;

    @Mock
    private BlobProperties blobProperties;

    @BeforeEach
    void setUp() {
        ocrValidationRetryManager = new OcrValidationRetryManager(
            OCR_VALIDATION_MAX_RETRIES,
            OCR_VALIDATION_RETRY_DELAY_SEC
        );
    }

    @Test
    void isReadyToRetry_should_return_true_if_delay_expiration_has_not_been_set() {
        // given
        final Map<String, String> metadata = emptyMap();
        given(blobClient.getProperties()).willReturn(blobProperties);
        given(blobProperties.getMetadata()).willReturn(metadata);

        // when
        // then
        assertThat(ocrValidationRetryManager.isReadyToRetry(blobClient)).isTrue();
    }

    @Test
    void isReadyToRetry_should_return_false_if_delay_expiration_time_has_not_passed() {
        // given
        given(blobClient.getProperties()).willReturn(blobProperties);
        final var metadata = Map.of(
            "ocrValidationRetryCount",
            "1",
            "ocrValidationRetryDelayExpirationTime",
            now(EUROPE_LONDON_ZONE_ID).plusSeconds(10).toString()
        );
        given(blobProperties.getMetadata()).willReturn(metadata);

        // when
        // then
        assertThat(ocrValidationRetryManager.isReadyToRetry(blobClient)).isFalse();
    }

    @Test
    void isReadyToRetry_should_return_true_if_delay_expiration_time_has_passed() {
        // given
        given(blobClient.getProperties()).willReturn(blobProperties);
        final var metadata = Map.of(
            "ocrValidationRetryCount",
            "1",
            "ocrValidationRetryDelayExpirationTime",
            now(EUROPE_LONDON_ZONE_ID).minusSeconds(1).toString()
        );
        given(blobProperties.getMetadata()).willReturn(metadata);

        // when
        // then
        assertThat(ocrValidationRetryManager.isReadyToRetry(blobClient)).isTrue();
    }

    @Test
    void setRetryDelayIfPossible_should_return_true_if_no_retry() {
        // given
        given(blobClient.getProperties()).willReturn(blobProperties);
        final Map<String, String> metadata = new HashMap<>();
        given(blobProperties.getMetadata()).willReturn(metadata);

        // when
        boolean res = ocrValidationRetryManager.setRetryDelayIfPossible(blobClient, "leaseId");

        // then
        assertThat(res).isTrue();
        assertThat(metadata.get("ocrValidationRetryCount")).isEqualTo("1");
        LocalDateTime retryDelayExpiresAt = parse(metadata.get("ocrValidationRetryDelayExpirationTime"));
        assertThat(retryDelayExpiresAt.isAfter(now(EUROPE_LONDON_ZONE_ID))).isTrue();
    }

    @Test
    void setRetryDelayIfPossible_should_return_true_if_first_retry() {
        // given
        given(blobClient.getProperties()).willReturn(blobProperties);
        final Map<String, String> metadata = new HashMap<>();
        metadata.put("ocrValidationRetryCount", "1");
        metadata.put("ocrValidationRetryDelayExpirationTime", now(EUROPE_LONDON_ZONE_ID).minusSeconds(1).toString());
        given(blobProperties.getMetadata()).willReturn(metadata);

        // when
        boolean res = ocrValidationRetryManager.setRetryDelayIfPossible(blobClient, "leaseId");

        // then
        assertThat(res).isTrue();
        assertThat(metadata.get("ocrValidationRetryCount")).isEqualTo("2");
        LocalDateTime retryDelayExpiresAt = parse(metadata.get("ocrValidationRetryDelayExpirationTime"));
        assertThat(retryDelayExpiresAt.isAfter(now(EUROPE_LONDON_ZONE_ID))).isTrue();
        verify(blobClient)
            .setMetadataWithResponse(
                eq(metadata),
                any(BlobRequestConditions.class),
                eq(null),
                eq(Context.NONE)
            );
    }

    @Test
    void setRetryDelayIfPossible_should_return_true_if_second_retry() {
        // given
        given(blobClient.getProperties()).willReturn(blobProperties);
        final Map<String, String> metadata = new HashMap<>();
        metadata.put("ocrValidationRetryCount", "2");
        metadata.put("ocrValidationRetryDelayExpirationTime", now(EUROPE_LONDON_ZONE_ID).minusSeconds(1).toString());
        given(blobProperties.getMetadata()).willReturn(metadata);

        // when
        boolean res = ocrValidationRetryManager.setRetryDelayIfPossible(blobClient, "leaseId");

        // then
        assertThat(res).isTrue();
        assertThat(metadata.get("ocrValidationRetryCount")).isEqualTo("3");
        LocalDateTime retryDelayExpiresAt = parse(metadata.get("ocrValidationRetryDelayExpirationTime"));
        assertThat(retryDelayExpiresAt.isAfter(now(EUROPE_LONDON_ZONE_ID))).isTrue();
        verify(blobClient)
            .setMetadataWithResponse(
                eq(metadata),
                any(BlobRequestConditions.class),
                eq(null),
                eq(Context.NONE)
            );
    }

    @Test
    void setRetryDelayIfPossible_should_return_false_if_max_number_of_retries_exceeded() {
        // given
        given(blobClient.getProperties()).willReturn(blobProperties);
        final Map<String, String> metadata = new HashMap<>();
        metadata.put("ocrValidationRetryCount", "3");
        metadata.put("ocrValidationRetryDelayExpirationTime", now(EUROPE_LONDON_ZONE_ID).minusSeconds(1).toString());
        given(blobProperties.getMetadata()).willReturn(metadata);

        // when
        boolean res = ocrValidationRetryManager.setRetryDelayIfPossible(blobClient, "leaseId");

        // then
        assertThat(res).isFalse();
        verify(blobClient, never()).setMetadataWithResponse(any(), any(), any(), any());
    }
}
