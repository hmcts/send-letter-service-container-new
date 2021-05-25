package uk.gov.hmcts.reform.sendletter.blob.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sendletter.model.in.DeleteBlob;
import uk.gov.hmcts.reform.sendletter.services.SasTokenGeneratorService;

@Component
public class BlobDelete {

    private static final Logger LOG = LoggerFactory.getLogger(BlobDelete.class);
    private final SasTokenGeneratorService sasTokenGeneratorService;
    private final BlobManager blobManager;

    public BlobDelete(BlobManager blobManager, SasTokenGeneratorService sasTokenGeneratorService) {
        this.blobManager =  blobManager;
        this.sasTokenGeneratorService  = sasTokenGeneratorService;
    }

    public boolean deleteOriginalBlobs(DeleteBlob deleteBlob) {
        var serviceName = deleteBlob.getServiceName();
        var containerName = deleteBlob.getContainerName();
        var blobs = deleteBlob.getBlobName();

        LOG.info("DeleteBlob:: serviceName {}, containerName {}", serviceName, containerName);
        var sasToken = sasTokenGeneratorService.generateSasToken(serviceName);

        for (String blob : blobs) {
            LOG.info("Deleting blob: {}", blob);
            doDelete(blob, sasToken, containerName);
        }
        return true;
    }

    private void doDelete(String pdfFile, String sasToken, String sourceContainerName) {
        LOG.info("About to delete original blob from {} container", sourceContainerName);
        var  sourceBlobClient = blobManager.getBlobClient(sourceContainerName, sasToken, pdfFile);
        var blob = sourceBlobClient.getBlobUrl();
        sourceBlobClient.delete();
        LOG.info("Blob {} delete successfully.", blob);
    }
}
