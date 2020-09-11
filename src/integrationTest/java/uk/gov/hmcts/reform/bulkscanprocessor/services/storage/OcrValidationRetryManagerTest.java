package uk.gov.hmcts.reform.bulkscanprocessor.services.storage;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.specialized.BlobLeaseClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.bulkscanprocessor.config.IntegrationTest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static java.time.LocalDateTime.now;
import static java.time.LocalDateTime.parse;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.bulkscanprocessor.util.TimeZones.EUROPE_LONDON_ZONE_ID;

@IntegrationTest
class OcrValidationRetryManagerTest {

    @Autowired
    private OcrValidationRetryManager ocrValidationRetryManager;

    @MockBean
    private LeaseClientProvider leaseClientProvider;

    @MockBean
    private LeaseMetaDataChecker leaseMetaDataChecker;

    @Mock
    private BlobClient blobClient;

    @Mock
    private BlobLeaseClient blobLeaseClient;

    @Mock
    private BlobProperties blobProperties;

    @BeforeEach
    void setUp() {
        given(blobClient.getProperties()).willReturn(blobProperties);
    }

    @Test
    void isReadyToRetry_should_return_true_if_delay_expiration_has_not_been_set() {
        // given
        final Map<String, String> metadata = emptyMap();
        given(blobProperties.getMetadata()).willReturn(metadata);

        // when
        // then
        assertThat(ocrValidationRetryManager.isReadyToRetry(blobClient)).isTrue();
    }

    @Test
    void isReadyToRetry_should_return_false_if_delay_expiration_time_has_not_passed() {
        // given
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
    void setRetryDelayIfPossible_should_return_true_if_first_retry() {
        // given
        final Map<String, String> metadata = new HashMap<>();
        given(blobProperties.getMetadata()).willReturn(metadata);
        given(leaseClientProvider.get(blobClient)).willReturn(blobLeaseClient);
        given(blobLeaseClient.acquireLease(anyInt())).willReturn("leaseId");
        given(leaseMetaDataChecker.isReadyToUse(blobClient,"leaseId")).willReturn(true);

        // when
        boolean res = ocrValidationRetryManager.setRetryDelayIfPossible(blobClient);

        // then
        assertThat(res).isTrue();
        assertThat(metadata.get("ocrValidationRetryCount")).isEqualTo("1");
        LocalDateTime retryDelayExpiresAt = parse(metadata.get("ocrValidationRetryDelayExpirationTime"));
        assertThat(retryDelayExpiresAt.isAfter(now(EUROPE_LONDON_ZONE_ID))).isTrue();
    }

    @Test
    void setRetryDelayIfPossible_should_return_true_if_second_retry() {
        // given
        final Map<String, String> metadata = new HashMap<>();
        metadata.put("ocrValidationRetryCount", "1");
        metadata.put("ocrValidationRetryDelayExpirationTime", now(EUROPE_LONDON_ZONE_ID).minusSeconds(1).toString());
        given(blobProperties.getMetadata()).willReturn(metadata);
        given(leaseClientProvider.get(blobClient)).willReturn(blobLeaseClient);
        given(blobLeaseClient.acquireLease(anyInt())).willReturn("leaseId");
        given(leaseMetaDataChecker.isReadyToUse(blobClient,"leaseId")).willReturn(true);

        // when
        boolean res = ocrValidationRetryManager.setRetryDelayIfPossible(blobClient);

        // then
        assertThat(res).isTrue();
        assertThat(metadata.get("ocrValidationRetryCount")).isEqualTo("2");
        LocalDateTime retryDelayExpiresAt = parse(metadata.get("ocrValidationRetryDelayExpirationTime"));
        assertThat(retryDelayExpiresAt.isAfter(now(EUROPE_LONDON_ZONE_ID))).isTrue();
    }

    @Test
    void setRetryDelayIfPossible_should_return_false_if_max_number_of_retries_exceeded() {
        // given
        final Map<String, String> metadata = new HashMap<>();
        metadata.put("ocrValidationRetryCount", "2");
        metadata.put("ocrValidationRetryDelayExpirationTime", now(EUROPE_LONDON_ZONE_ID).minusSeconds(1).toString());
        given(blobProperties.getMetadata()).willReturn(metadata);

        // when
        boolean res = ocrValidationRetryManager.setRetryDelayIfPossible(blobClient);

        // then
        assertThat(res).isFalse();
        verify(blobClient, never()).setMetadata(metadata);
    }

}
