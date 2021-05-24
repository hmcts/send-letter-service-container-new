package uk.gov.hmcts.reform.sendletter.blob;

import com.azure.core.util.Context;
import com.azure.storage.blob.models.BlobRequestConditions;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.models.DeleteSnapshotsOptionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sendletter.blob.component.BlobBackup;
import uk.gov.hmcts.reform.sendletter.blob.component.BlobDelete;
import uk.gov.hmcts.reform.sendletter.blob.component.BlobManager;
import uk.gov.hmcts.reform.sendletter.blob.component.BlobReader;
import uk.gov.hmcts.reform.sendletter.blob.component.BlobStitch;
import uk.gov.hmcts.reform.sendletter.blob.storage.LeaseClientProvider;
import uk.gov.hmcts.reform.sendletter.model.in.ManifestBlobInfo;

import java.io.IOException;
import java.util.Optional;

@Service
public class BlobProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(BlobProcessor.class);

    private final BlobManager blobManager;
    private final BlobReader blobReader;
    private final BlobBackup blobBackup;
    private final BlobStitch blobStitch;
    private final BlobDelete blobDelete;
    private final LeaseClientProvider leaseClientProvider;
    private final Integer leaseTime;

    public BlobProcessor(BlobReader blobReader, BlobBackup blobBackup, BlobStitch blobStitch,
                         BlobDelete blobDelete, BlobManager blobManager,
                         LeaseClientProvider leaseClientProvider,
                         @Value("${storage.leaseTime}") Integer leaseTime) {
        this.blobReader = blobReader;
        this.blobBackup = blobBackup;
        this.blobStitch = blobStitch;
        this.blobDelete = blobDelete;
        this.blobManager = blobManager;
        this.leaseClientProvider =  leaseClientProvider;
        this.leaseTime = leaseTime;
    }

    public boolean read() throws IOException {
        LOG.info("BlobProcessor:: keda proccessing blobs");
        Optional<ManifestBlobInfo> blobInfo = blobReader.retrieveManifestsToProcess();
        if (blobInfo.isPresent()) {
            try {
                var containerClient  = blobManager.getContainerClient(blobInfo.get().getContainerName());
                var blobClient = containerClient.getBlobClient(blobInfo.get().getBlobName());
                var leaseClient = leaseClientProvider.get(blobClient);
                var leaseId = leaseClient.acquireLease(leaseTime);
                LOG.info("BlobProcessor::blob {} has been leased for {} seconds with leaseId {}",
                        blobInfo.get().getBlobName(), leaseTime, leaseId);
                var printResponse = blobBackup.backupBlobs(blobInfo.get());
                LOG.info("BlobProcessor:: backup blobs response {}", printResponse);
                var deleteBlob = blobStitch.stitchBlobs(printResponse);
                blobClient.deleteWithResponse(
                        DeleteSnapshotsOptionType.INCLUDE,
                        new BlobRequestConditions().setLeaseId(leaseId),
                        null,
                        Context.NONE);
                blobDelete.deleteOriginalBlobs(deleteBlob);
                LOG.info("BlobProcessor:: delete original blobs");
            } catch (BlobStorageException bse) {
                LOG.error("There is already a lease present for blob {}", blobInfo.get().getBlobName(), bse);
            }
        } else {
            LOG.info("BlobProcessor:: no blobs for proccessing.");
            return false;
        }

        return true;
    }
}
