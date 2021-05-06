package uk.gov.hmcts.reform.sendletter.blob.component;

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
        var sasToken = sasTokenGeneratorService.generateSasToken("bulkprint");
        LOG.info("BlobBackup:: sasToken: {}", sasToken);

        var sourceBlobClient = new BlobClientBuilder()
                .endpoint(blobManager.getAccountUrl())
                .sasToken(sasToken)
                .containerName(NEW_CONTAINER)
                .blobName(originalBlob)
                .buildClient();

        var destBlobClient = destContainerClient.getBlobClient(originalBlob);
        String copyId = destBlobClient.copyFromUrl(sourceBlobClient.getBlobUrl() + "?" + sasToken);
        LOG.info("Blob backup complete copy id {} ", copyId);
        return copyId;
    }

}
