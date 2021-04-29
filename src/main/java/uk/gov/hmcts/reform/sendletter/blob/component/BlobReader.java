package uk.gov.hmcts.reform.sendletter.blob.component;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class BlobReader {
    private static final Logger LOG = LoggerFactory.getLogger(BlobReader.class);
    private static final String NEW_CONTAINER = "new";
    private final String connection;
    private String blobName;

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

        Optional<BlobItem> first = containerClient.listBlobs().stream().findFirst();
        if (first.isPresent()) {
            blobName = first.get().getName();
        }
        LOG.info("BlobReader:: Blob name: {}", blobName);

        return blobName;
    }
}
