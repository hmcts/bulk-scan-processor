package uk.gov.hmcts.reform.bulkscanprocessor.services.storage;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.models.BlobErrorCode;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.specialized.BlobLeaseClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class LeaseAcquirerTest {

    @Mock private BlobClient blobClient;
    @Mock private BlobLeaseClient leaseClient;
    @Mock private BlobStorageException blobStorageException;

    @Mock private LeaseMetaDataChecker leaseMetaDataChecker;

    private String leaseId = "Lease-id-123";
    private LeaseAcquirer leaseAcquirer;

    @BeforeEach
    void setUp() {
        leaseAcquirer = new LeaseAcquirer(blobClient -> leaseClient, leaseMetaDataChecker);
    }

    @Test
    void should_run_provided_action_when_lease_was_acquired() {
        // given
        var onSuccess = mock(Consumer.class);
        var onFailure = mock(Consumer.class);

        given(leaseMetaDataChecker.isReadyToUse(any(),any())).willReturn(true);
        given(leaseClient.acquireLease(anyInt())).willReturn(leaseId);
        // when
        leaseAcquirer.ifAcquiredOrElse(blobClient, onSuccess, onFailure, false);

        // then
        verify(onSuccess).accept(leaseId);
        verify(onFailure, never()).accept(any(BlobErrorCode.class));
        verify(leaseMetaDataChecker).isReadyToUse(eq(blobClient), eq(leaseId));
        verifyNoMoreInteractions(leaseMetaDataChecker);
    }

    @Test
    void should_run_onFailure_action_when_there_is_error() {
        // given
        var onSuccess = mock(Consumer.class);
        var onFailure = mock(Consumer.class);

        doThrow(blobStorageException).when(leaseClient).acquireLease(anyInt());

        // when
        leaseAcquirer.ifAcquiredOrElse(blobClient, onSuccess, onFailure, false);

        // then
        verify(onSuccess, never()).accept(anyString());
        verify(onFailure).accept(null);
    }

    @Test
    void should_not_call_release_when_failed_to_process_blob() {
        // given
        doThrow(blobStorageException).when(leaseClient).acquireLease(anyInt());

        // when
        leaseAcquirer.ifAcquiredOrElse(blobClient, mock(Consumer.class), mock(Consumer.class), true);

        // then
        verify(leaseClient, never()).releaseLease();
    }

    @Test
    void should_call_release_when_successfully_processed_blob() {
        //given
        given(leaseMetaDataChecker.isReadyToUse(any(),any())).willReturn(true);
        given(leaseClient.acquireLease(anyInt())).willReturn(leaseId);
        given(leaseClient.getLeaseId()).willReturn(leaseId);

        // when
        leaseAcquirer.ifAcquiredOrElse(blobClient, mock(Consumer.class), mock(Consumer.class), true);

        // then
        verify(leaseClient).releaseLease();
        verify(leaseMetaDataChecker).isReadyToUse(eq(blobClient), eq(leaseId));
        verify(leaseMetaDataChecker).clearMetaData(eq(blobClient), eq(leaseId));
        verifyNoMoreInteractions(leaseMetaDataChecker);
    }


    @Test
    void should_not_run_provided_action_when_metadata_lease_was_not_ready() {
        // given
        var onSuccess = mock(Consumer.class);
        var onFailure = mock(Consumer.class);

        given(leaseMetaDataChecker.isReadyToUse(any(),any())).willReturn(false);

        // when
        leaseAcquirer.ifAcquiredOrElse(blobClient, onSuccess, onFailure, false);

        // then
        verify(leaseClient).releaseLease();
        verify(onSuccess, never()).accept(anyString());
        verify(onFailure, never()).accept(any());
    }


    @Test
    void should_release_lease_when_metadata_lease_check_was_throw_exception() {
        // given
        var onSuccess = mock(Consumer.class);
        var onFailure = mock(Consumer.class);

        given(leaseMetaDataChecker.isReadyToUse(any(),any()))
            .willThrow(new RuntimeException("Can not write to Metadata"));
        given(leaseClient.acquireLease(anyInt())).willReturn(leaseId);

        // when
        leaseAcquirer.ifAcquiredOrElse(blobClient, onSuccess, onFailure, true);

        // then
        verify(leaseClient).releaseLease();
        verify(onSuccess, never()).accept(anyString());
        verify(onFailure, never()).accept(any());
        verify(leaseMetaDataChecker).isReadyToUse(eq(blobClient), eq(leaseId));
        verifyNoMoreInteractions(leaseMetaDataChecker);

    }

    @Test
    void should_catch_exception_when_metadata_lease_clear_throw_exception() {
        // given
        given(leaseMetaDataChecker.isReadyToUse(any(),any())).willReturn(true);
        willThrow(new BlobStorageException("Can not clear metadata", null, null))
            .given(leaseMetaDataChecker).clearMetaData(any(),any());
        given(leaseClient.getLeaseId()).willReturn(leaseId);
        given(leaseClient.acquireLease(anyInt())).willReturn(leaseId);

        var onSuccess = mock(Consumer.class);
        var onFailure = mock(Consumer.class);


        // when
        leaseAcquirer.ifAcquiredOrElse(blobClient, onSuccess, onFailure, true);

        // then
        verify(onSuccess).accept(anyString());
        verify(onFailure, never()).accept(any());
        verify(leaseClient).acquireLease(anyInt());
        verify(leaseClient, times(2)).getLeaseId();
        verifyNoMoreInteractions(leaseClient);
        verify(leaseMetaDataChecker).isReadyToUse(eq(blobClient), eq(leaseId));
        verify(leaseMetaDataChecker).clearMetaData(eq(blobClient), eq(leaseId));
        verifyNoMoreInteractions(leaseMetaDataChecker);

    }

}
