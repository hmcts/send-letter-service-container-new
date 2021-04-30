package uk.gov.hmcts.reform.sendletter.blob.component;

import com.azure.storage.blob.models.BlobItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class BlobReader {

    private static final Logger LOG = LoggerFactory.getLogger(BlobReader.class);

    private final BlobManager blobManager;
    private static final String NEW_CONTAINER = "new";

    public BlobReader(BlobManager blobManager) {
        this.blobManager =  blobManager;
    }

    public String retrieveFileToProcess() {
        LOG.info("About to read new blob connection details");
        var containerClient = blobManager.listContainerClient(NEW_CONTAINER);
        String blobName = containerClient.listBlobs().stream().findFirst().map(BlobItem::getName).orElse("");
        LOG.info("BlobReader:: Blob name: {}", blobName);
        return blobName;
    }


}
