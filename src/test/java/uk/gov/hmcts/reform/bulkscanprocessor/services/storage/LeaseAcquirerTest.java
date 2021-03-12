package uk.gov.hmcts.reform.bulkscanprocessor.services.storage;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.models.BlobErrorCode;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.models.CopyStatusType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.hmcts.reform.bulkscanprocessor.services.storage.LeaseMetaDataChecker.LEASE_EXPIRATION_TIME;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class LeaseAcquirerTest {

    @Mock private BlobClient blobClient;
    @Mock private BlobProperties blobProperties;
    @Mock private BlobStorageException blobStorageException;

    @Mock private LeaseMetaDataChecker leaseMetaDataChecker;

    private LeaseAcquirer leaseAcquirer;

    @BeforeEach
    void setUp() {
        leaseAcquirer = new LeaseAcquirer(leaseMetaDataChecker);
    }

    @Test
    void should_run_provided_action_when_lease_was_acquired() {
        // given
        setCopyStatus(null);
        given(leaseMetaDataChecker.isReadyToUse(any())).willReturn(true);
        var onSuccess = mock(Consumer.class);
        var onFailure = mock(Consumer.class);
        // when
        leaseAcquirer.ifAcquiredOrElse(blobClient, onSuccess, onFailure, false);

        // then
        verify(onSuccess).accept(null);
        verify(onFailure, never()).accept(any(BlobErrorCode.class));
        verify(leaseMetaDataChecker).isReadyToUse(eq(blobClient));
        verifyNoMoreInteractions(leaseMetaDataChecker);
    }

    @Test
    void should_run_onFailure_action_when_there_is_error() {
        // given
        var onSuccess = mock(Consumer.class);
        var onFailure = mock(Consumer.class);
        setCopyStatus(null);
        doThrow(blobStorageException).when(leaseMetaDataChecker).isReadyToUse(any());

        // when
        leaseAcquirer.ifAcquiredOrElse(blobClient, onSuccess, onFailure, false);

        // then
        verify(onSuccess, never()).accept(anyString());
        verify(onFailure).accept(null);
    }

    @Test
    void should_not_call_release_when_failed_to_process_blob() {
        // given
        setCopyStatus(CopyStatusType.SUCCESS);

        doThrow(blobStorageException).when(leaseMetaDataChecker).isReadyToUse(any());

        // when
        leaseAcquirer.ifAcquiredOrElse(blobClient, mock(Consumer.class), mock(Consumer.class), true);

        // then
        verify(leaseMetaDataChecker, never()).clearMetaData(any());
    }

    @Test
    void should_call_release_when_successfully_processed_blob() {
        //given
        given(leaseMetaDataChecker.isReadyToUse(any())).willReturn(true);

        given(blobClient.getProperties()).willReturn(blobProperties);
        final Map<String, String> metadata = new HashMap<>();
        metadata.put(LEASE_EXPIRATION_TIME, "time");
        metadata.put("someProperty", "someValue");
        given(blobProperties.getMetadata()).willReturn(metadata);
        given(blobProperties.getCopyStatus()).willReturn(null);

        // when
        leaseAcquirer.ifAcquiredOrElse(blobClient, mock(Consumer.class), mock(Consumer.class), true);

        // then
        verify(leaseMetaDataChecker).isReadyToUse(eq(blobClient));
        verify(blobClient).setMetadata(eq(metadata));
        assertThat(metadata.containsKey(LEASE_EXPIRATION_TIME)).isFalse();
        assertThat(metadata.containsKey("someProperty")).isTrue();
        assertThat(metadata.get("someProperty")).isEqualTo("someValue");
        verifyNoMoreInteractions(leaseMetaDataChecker);
    }


    @Test
    void should_not_run_provided_action_when_metadata_lease_was_not_ready() {
        // given
        var onSuccess = mock(Consumer.class);
        var onFailure = mock(Consumer.class);

        setCopyStatus(CopyStatusType.SUCCESS);

        given(leaseMetaDataChecker.isReadyToUse(any())).willReturn(false);

        // when
        leaseAcquirer.ifAcquiredOrElse(blobClient, onSuccess, onFailure, false);

        // then
        verify(onSuccess, never()).accept(anyString());
        verify(onFailure).accept(any());
    }


    @Test
    void should_catch_exception_when_metadata_lease_clear_throw_exception() {
        // given
        given(leaseMetaDataChecker.isReadyToUse(any())).willReturn(true);
        willThrow(new BlobStorageException("Can not clear metadata", null, null))
            .given(blobClient).setMetadata(any());

        given(blobClient.getProperties()).willReturn(blobProperties);
        final Map<String, String> metadata = new HashMap<>();
        metadata.put(LEASE_EXPIRATION_TIME, "time");
        metadata.put("someProperty", "someValue");
        given(blobProperties.getMetadata()).willReturn(metadata);
        given(blobProperties.getCopyStatus()).willReturn(CopyStatusType.SUCCESS);

        var onSuccess = mock(Consumer.class);
        var onFailure = mock(Consumer.class);

        // when
        leaseAcquirer.ifAcquiredOrElse(blobClient, onSuccess, onFailure, true);

        // then
        verify(onSuccess).accept(null);
        verify(onFailure, never()).accept(any());
        verify(leaseMetaDataChecker).isReadyToUse(eq(blobClient));
        verify(blobClient).setMetadata(eq(metadata));
        assertThat(metadata.containsKey(LEASE_EXPIRATION_TIME)).isFalse();
        assertThat(metadata.containsKey("someProperty")).isTrue();
        assertThat(metadata.get("someProperty")).isEqualTo("someValue");
        verifyNoMoreInteractions(leaseMetaDataChecker);

    }

    @Test
    void should_run_onFailure_when_metadata_lease_can_not_acquired() {
        // given
        setCopyStatus(CopyStatusType.SUCCESS);
        given(leaseMetaDataChecker.isReadyToUse(any())).willReturn(false);

        var onSuccess = mock(Consumer.class);
        Consumer<BlobErrorCode> onFailure = errorCode -> {
            throw new IllegalStateException("Metadata Lease failed. ErrorCode: " + errorCode.toString());
        };

        // when
        assertThrows(
            IllegalStateException.class,
            () -> leaseAcquirer.ifAcquiredOrElse(blobClient, onSuccess, onFailure, true));

        // then
        verify(onSuccess, never()).accept(anyString());
        verify(leaseMetaDataChecker).isReadyToUse(eq(blobClient));
        verifyNoMoreInteractions(leaseMetaDataChecker);

    }

    @ParameterizedTest
    @EnumSource(
        value = CopyStatusType.class,
        names = {"PENDING", "ABORTED", "FAILED"}
    )
    void should_skip_lease_when_copy_status_is_not_success(CopyStatusType copyStatus) {
        // given
        var onSuccess = mock(Consumer.class);
        var onFailure = mock(Consumer.class);
        setCopyStatus(copyStatus);

        // when
        leaseAcquirer.ifAcquiredOrElse(blobClient, onSuccess, onFailure, false);

        // then
        verify(onSuccess, never()).accept(anyString());
        verify(onFailure, never()).accept(any());
        verifyNoMoreInteractions(leaseMetaDataChecker);

    }

    @Test
    void should_skip_lease_when_meta_data_has_copy_waiting() {
        // given
        BlobProperties blobItemProperties = mock(BlobProperties.class);
        given(blobItemProperties.getCopyStatus()).willReturn(null);
        given(blobClient.getProperties()).willReturn(blobItemProperties);

        given(blobItemProperties.getMetadata())
            .willReturn(Map.of("waitingCopy", "anyValue"));
        var onSuccess = mock(Consumer.class);
        var onFailure = mock(Consumer.class);

        // when
        leaseAcquirer.ifAcquiredOrElse(blobClient, onSuccess, onFailure, false);

        // then
        verify(onSuccess, never()).accept(anyString());
        verify(onFailure, never()).accept(any());
        verifyNoMoreInteractions(leaseMetaDataChecker);

    }

    private void setCopyStatus(CopyStatusType copyStatus) {
        BlobProperties blobItemProperties = mock(BlobProperties.class);
        given(blobItemProperties.getCopyStatus()).willReturn(copyStatus);
        given(blobClient.getProperties()).willReturn(blobItemProperties);
    }

}
