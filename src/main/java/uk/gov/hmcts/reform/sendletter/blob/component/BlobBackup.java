package uk.gov.hmcts.reform.sendletter.blob.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class BlobBackup {

    private static final Logger LOG = LoggerFactory.getLogger(BlobBackup.class);

    private final BlobManager blobManager;
    private static final String NEW_CONTAINER = "new";
    private static final String BACKUP_CONTAINER = "backup";

    public BlobBackup(BlobManager blobManager) {
        this.blobManager =  blobManager;
    }

    public String backupBlob(String originalBlob) {
        LOG.info("About to backup original blob in backup container");
        var sourceContainerClient = blobManager.listContainerClient(NEW_CONTAINER);
        var destContainerClient = blobManager.listContainerClient(BACKUP_CONTAINER);
        var sourceBlobClient = sourceContainerClient.getBlobClient(originalBlob);
        var destBlobClient = destContainerClient.getBlobClient(originalBlob);
        String copyId = destBlobClient.copyFromUrl(sourceBlobClient.getBlobUrl());
        LOG.info("Blob backup complete copy id {} ", copyId);
        return copyId;
    }


}
