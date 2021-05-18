package uk.gov.hmcts.reform.sendletter.model.in;

import java.util.List;

public class DeleteBlob {

    private String serviceName;
    private String containerName;
    private List<String> blobName;

    public DeleteBlob() {
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    public void setBlobName(List<String> blobName) {
        this.blobName = blobName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getContainerName() {
        return containerName;
    }

    public List<String> getBlobName() {
        return blobName;
    }

}
