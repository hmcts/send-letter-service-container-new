package uk.gov.hmcts.reform.sendletter.blob.component;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BlobReader {

    private static final Logger LOG = LoggerFactory.getLogger(BlobReader.class);
    private static final String NEW_CONTAINER = "new";
    private final String connection;


    public BlobReader(@Value("${storage.connection}") String connection) {
        this.connection = connection;
    }

    public String retrieveFileToProcess() {
        LOG.info("About to read new blob connection details");
        if (connection != null) {
            LOG.info("Blog connection found");
        }
        // Create a BlobServiceClient object which will be used to create a container client
        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder().connectionString(connection).buildClient();
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(NEW_CONTAINER);

        String blobName = containerClient.listBlobs().stream().findFirst().map(blob -> blob.getName()).orElse("");
        LOG.info("BlobReader:: Blob name: {}", blobName);
        return blobName;
    }
}
