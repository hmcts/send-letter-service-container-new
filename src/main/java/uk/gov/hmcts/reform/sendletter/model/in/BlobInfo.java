package uk.gov.hmcts.reform.sendletter.model.in;

import com.azure.storage.blob.BlobClient;

import java.util.Optional;

public class BlobInfo {
    private final BlobClient blobClient;
    private String containerName;
    private Optional<String> leaseId;

    public BlobInfo(BlobClient blobClient) {
        this.blobClient = blobClient;
        this.leaseId = Optional.empty();
        this.containerName = "";
    }

    public BlobClient getBlobClient() {
        return blobClient;
    }

    public Optional<String> getLeaseId() {
        return leaseId;
    }

    public boolean isLeased() {
        return leaseId.isPresent();
    }

    public void setLeaseId(String leaseId) {
        this.leaseId = Optional.ofNullable(leaseId);
    }

    public String getContainerName() {
        return containerName;
    }
    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }
}
