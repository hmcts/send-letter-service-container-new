package uk.gov.hmcts.reform.sendletter.blob;

import com.azure.core.util.Context;
import com.azure.storage.blob.models.BlobRequestConditions;
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

    public boolean read() {
        LOG.info("BlobProcessor:: proccessing blob");

        return blobReader.retrieveManifestsToProcess()
                .stream()
                .map(this::process)
                .filter(Boolean::valueOf)
                .findFirst()
                .orElse(false);
    }

    private boolean process(ManifestBlobInfo manifestBlobInfo) {
        boolean status = false;
        try {
            var containerClient  = blobManager.getContainerClient(manifestBlobInfo.getContainerName());
            var blobClient = containerClient.getBlobClient(manifestBlobInfo.getBlobName());
            var leaseClient = leaseClientProvider.get(blobClient);
            var leaseId = leaseClient.acquireLease(leaseTime);
            LOG.info("BlobProcessor::blob {} has been leased for {} seconds with leaseId {}",
                    manifestBlobInfo.getBlobName(), leaseTime, leaseId);
            var printResponse = blobBackup.backupBlobs(manifestBlobInfo);
            LOG.info("BlobProcessor:: backup blobs response {}", printResponse);
            var deleteBlob = blobStitch.stitchBlobs(printResponse);
            blobClient.deleteWithResponse(
                    DeleteSnapshotsOptionType.INCLUDE,
                    new BlobRequestConditions().setLeaseId(leaseId),
                    null,
                    Context.NONE);
            status = blobDelete.deleteOriginalBlobs(deleteBlob);
            LOG.info("BlobProcessor:: delete original blobs");
        } catch (Exception e) {
            LOG.error("Exception processing blob {}", manifestBlobInfo.getBlobName(), e);
        }
        return status;
    }
}
