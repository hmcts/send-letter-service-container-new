package uk.gov.hmcts.reform.sendletter.model.in;

public class ManifestBlobInfo {

    private String serviceName;
    private String containerName;
    private String blobName;

    public ManifestBlobInfo(String serviceName, String containerName, String blobName) {
        this.serviceName = serviceName;
        this.containerName = containerName;
        this.blobName = blobName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getContainerName() {
        return containerName;
    }

    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    public String getBlobName() {
        return blobName;
    }

    public void setBlobName(String blobName) {
        this.blobName = blobName;
    }
}
