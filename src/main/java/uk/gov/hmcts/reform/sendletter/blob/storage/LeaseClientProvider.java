package uk.gov.hmcts.reform.sendletter.blob.storage;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.specialized.BlobLeaseClient;

public interface LeaseClientProvider {
    BlobLeaseClient get(BlobClient blobClient);
}
