package uk.gov.hmcts.reform.sendletter.blob.component;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobClientBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import org.springframework.stereotype.Component;

@Component
public class BlobManager {

    private final BlobServiceClient blobServiceClient;

    public BlobManager(BlobServiceClient blobServiceClient) {
        this.blobServiceClient = blobServiceClient;
    }

    public BlobContainerClient getContainerClient(String containerName) {
        return blobServiceClient.getBlobContainerClient(containerName);
    }

    public BlobClient getBlobClient(String containerName, String sasToken, String blobName) {
        return new BlobClientBuilder()
                .endpoint(getAccountUrl())
                .sasToken(sasToken)
                .containerName(containerName)
                .blobName(blobName)
                .buildClient();
    }

    public String getAccountUrl() {
        return blobServiceClient.getAccountUrl();
    }
}