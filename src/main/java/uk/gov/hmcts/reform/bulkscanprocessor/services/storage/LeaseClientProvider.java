package uk.gov.hmcts.reform.bulkscanprocessor.services.storage;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.specialized.BlobLeaseClient;

/**
 * Provides a lease client for a given blob client.
 */
public interface LeaseClientProvider {
    BlobLeaseClient get(BlobClient blobClient);
}
