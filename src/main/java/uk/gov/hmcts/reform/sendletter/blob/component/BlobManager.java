package uk.gov.hmcts.reform.sendletter.blob.component;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import org.springframework.stereotype.Component;

@Component
public class BlobManager {

    private final BlobServiceClient blobServiceClient;

    public BlobManager(BlobServiceClient blobServiceClient) {
        this.blobServiceClient = blobServiceClient;
    }

    public BlobContainerClient listContainerClient(String containerName) {
        return blobServiceClient.getBlobContainerClient(containerName);
    }
}