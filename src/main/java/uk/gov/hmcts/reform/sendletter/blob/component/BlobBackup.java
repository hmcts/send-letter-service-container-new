package uk.gov.hmcts.reform.sendletter.blob.component;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sendletter.services.SasTokenGeneratorService;

@Component
public class BlobBackup {

    private static final Logger LOG = LoggerFactory.getLogger(BlobBackup.class);

    private final SasTokenGeneratorService sasTokenGeneratorService;
    private final BlobManager blobManager;
    private static final String NEW_CONTAINER = "new";
    private static final String BACKUP_CONTAINER = "backup";

    public BlobBackup(BlobManager blobManager, SasTokenGeneratorService sasTokenGeneratorService) {
        this.blobManager =  blobManager;
        this.sasTokenGeneratorService  = sasTokenGeneratorService;
    }

    public String backupBlob(String originalBlob) {
        LOG.info("About to backup original blob in backup container");

        var destContainerClient = blobManager.getContainerClient(BACKUP_CONTAINER);

        BlobClient sourceBlobClient = new BlobClientBuilder()
                .endpoint(blobManager.getAccountUrl())
                .sasToken(sasTokenGeneratorService.generateSasToken("bulkprint"))
                .containerName(NEW_CONTAINER)
                .blobName(originalBlob)
                .buildClient();

        var destBlobClient = destContainerClient.getBlobClient(originalBlob);
        String copyId = destBlobClient.copyFromUrl(sourceBlobClient.getBlobUrl());
        LOG.info("Blob backup complete copy id {} ", copyId);
        return copyId;
    }


}
