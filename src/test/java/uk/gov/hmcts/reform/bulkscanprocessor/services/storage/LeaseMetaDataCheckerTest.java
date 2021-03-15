package uk.gov.hmcts.reform.bulkscanprocessor.services.storage;

import com.azure.core.util.Context;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.models.BlobRequestConditions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.config.BlobManagementProperties;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.bulkscanprocessor.util.TimeZones.EUROPE_LONDON_ZONE_ID;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class LeaseMetaDataCheckerTest {

    @Mock private BlobClient blobClient;
    @Mock private BlobProperties blobProperties;

    @Mock private BlobManagementProperties managementProperties;

    private Map<String, String> blobMetaData;

    private static final String LEASE_EXPIRATION_TIME = "leaseExpirationTime";

    private LeaseMetaDataChecker leaseMetaDataChecker;

    @BeforeEach
    void setUp() {
        leaseMetaDataChecker = new LeaseMetaDataChecker(managementProperties);
        blobMetaData = new HashMap<>();
    }

    @Test
    void should_return_true_when_no_expiry_in_metadata() {
        //given
        given(blobClient.getProperties()).willReturn(blobProperties);
        given(blobProperties.getMetadata()).willReturn(blobMetaData);
        String etag = "etag-21321312";
        given(blobProperties.getETag()).willReturn(etag);

        int leaseTimeoutISec = 300;
        given(managementProperties.getBlobLeaseAcquireDelayInSeconds()).willReturn(leaseTimeoutISec);
        LocalDateTime minExpiryTime = LocalDateTime.now(EUROPE_LONDON_ZONE_ID)
            .plusSeconds(leaseTimeoutISec);

        //when
        boolean isReady = leaseMetaDataChecker.isReadyToUse(blobClient);

        //then
        assertThat(isReady).isTrue();
        var mapCapturer = ArgumentCaptor.forClass(Map.class);
        var conditionCapturer = ArgumentCaptor.forClass(BlobRequestConditions.class);
        verify(blobClient)
            .setMetadataWithResponse(mapCapturer.capture(), conditionCapturer.capture(), eq(null), eq(Context.NONE));
        Map<String, String> map = mapCapturer.getValue();
        LocalDateTime leaseExpiresAt = LocalDateTime.parse(map.get(LEASE_EXPIRATION_TIME));
        assertThat(minExpiryTime).isBefore(leaseExpiresAt);
        BlobRequestConditions con = conditionCapturer.getValue();
        assertThat(con.getIfMatch()).isEqualTo("\"" + etag + "\"");
    }

    @Test
    void should_return_false_when_expiry_in_metadata_valid() {
        //given
        given(blobClient.getProperties()).willReturn(blobProperties);
        blobMetaData.put(LEASE_EXPIRATION_TIME, LocalDateTime.now(EUROPE_LONDON_ZONE_ID).plusSeconds(40).toString());
        given(blobProperties.getMetadata()).willReturn(blobMetaData);

        //when
        boolean isReady = leaseMetaDataChecker.isReadyToUse(blobClient);

        //then
        assertThat(isReady).isFalse();
        verify(blobClient,never()).setMetadataWithResponse(any(), any(), any(), any());

    }

    @Test
    void should_return_true_when_metadata_lease_expiration_expired() {
        //given
        given(blobClient.getProperties()).willReturn(blobProperties);
        blobMetaData.put(LEASE_EXPIRATION_TIME, LocalDateTime.now(EUROPE_LONDON_ZONE_ID).toString());

        given(blobProperties.getMetadata()).willReturn(blobMetaData);

        given(managementProperties.getBlobLeaseAcquireDelayInSeconds()).willReturn(15);
        String etag = "etag-21321312";
        given(blobProperties.getETag()).willReturn(etag);

        int leaseTimeoutISec = 10;
        given(managementProperties.getBlobLeaseAcquireDelayInSeconds()).willReturn(leaseTimeoutISec);
        LocalDateTime minExpiryTime = LocalDateTime.now(EUROPE_LONDON_ZONE_ID)
            .plusSeconds(leaseTimeoutISec);
        //when
        boolean isReady = leaseMetaDataChecker.isReadyToUse(blobClient);

        //then
        assertThat(isReady).isTrue();

        var mapCapturer = ArgumentCaptor.forClass(Map.class);
        var conditionCapturer = ArgumentCaptor.forClass(BlobRequestConditions.class);
        verify(blobClient)
            .setMetadataWithResponse(mapCapturer.capture(), conditionCapturer.capture(), eq(null), eq(Context.NONE));
        Map<String, String> map = mapCapturer.getValue();
        LocalDateTime leaseExpiresAt = LocalDateTime.parse(map.get(LEASE_EXPIRATION_TIME));
        assertThat(minExpiryTime).isBefore(leaseExpiresAt);
        BlobRequestConditions con = conditionCapturer.getValue();
        assertThat(con.getIfMatch()).isEqualTo("\"" + etag + "\"");

    }

    @Test
    void should_clear_blob_metadata_when_clear_successful() {
        //given
        given(blobClient.getProperties()).willReturn(blobProperties);
        blobMetaData.put(LEASE_EXPIRATION_TIME, LocalDateTime.now(EUROPE_LONDON_ZONE_ID).plusSeconds(40).toString());
        given(blobProperties.getMetadata()).willReturn(blobMetaData);

        doNothing().when(blobClient).setMetadata(any());

        //when
        leaseMetaDataChecker.clearMetaData(blobClient);

        //then
        var metaDataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(blobClient).setMetadata(metaDataCaptor.capture());

        assertThat(metaDataCaptor.getValue()).isEmpty();
    }
}
