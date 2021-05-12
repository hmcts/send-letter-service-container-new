package uk.gov.hmcts.reform.sendletter.model.in;

public class ManifestBlobInfo {

    private final String serviceName;
    private final String containerName;
    private final String blobName;

    public ManifestBlobInfo(String serviceName, String containerName, String blobName) {
        this.serviceName = serviceName;
        this.containerName = containerName;
        this.blobName = blobName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getContainerName() {
        return containerName;
    }

    public String getBlobName() {
        return blobName;
    }
}
