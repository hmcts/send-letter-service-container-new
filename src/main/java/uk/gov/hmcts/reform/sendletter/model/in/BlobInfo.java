package uk.gov.hmcts.reform.sendletter.model.in;

import com.azure.storage.blob.BlobClient;

import java.util.Optional;

public class BlobInfo {
    private final BlobClient blobClient;
    private Optional<String> containerName;
    private Optional<String> serviceName;
    private Optional<String> leaseId;

    public BlobInfo(BlobClient blobClient) {
        this.blobClient = blobClient;
        this.leaseId = Optional.empty();
        this.containerName = Optional.empty();
        this.serviceName = Optional.empty();
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
        return containerName.orElse("invalid container");
    }

    public void setContainerName(String containerName) {
        this.containerName = Optional.ofNullable(containerName);
    }

    public String getServiceName() {
        return serviceName.orElse("invalid service");
    }

    public void setServiceName(String serviceName) {
        this.serviceName = Optional.ofNullable(serviceName);
    }
}
