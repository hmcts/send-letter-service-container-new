package uk.gov.hmcts.reform.sendletter.blob;

import com.azure.core.util.Context;
import com.azure.storage.blob.models.BlobRequestConditions;
import com.azure.storage.blob.models.DeleteSnapshotsOptionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sendletter.blob.component.BlobBackup;
import uk.gov.hmcts.reform.sendletter.blob.component.BlobDelete;
import uk.gov.hmcts.reform.sendletter.blob.component.BlobReader;
import uk.gov.hmcts.reform.sendletter.blob.component.BlobStitch;
import uk.gov.hmcts.reform.sendletter.exceptions.BlobProcessException;
import uk.gov.hmcts.reform.sendletter.exceptions.LeaseIdNotPresentException;
import uk.gov.hmcts.reform.sendletter.model.in.BlobInfo;

@Service
public class BlobProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(BlobProcessor.class);

    private final BlobReader blobReader;
    private final BlobBackup blobBackup;
    private final BlobStitch blobStitch;
    private final BlobDelete blobDelete;

    public BlobProcessor(BlobReader blobReader, BlobBackup blobBackup, BlobStitch blobStitch,
                         BlobDelete blobDelete) {
        this.blobReader = blobReader;
        this.blobBackup = blobBackup;
        this.blobStitch = blobStitch;
        this.blobDelete = blobDelete;
    }

    public boolean read() {
        LOG.info("BlobProcessor:: processing blob");

        return blobReader.retrieveBlobToProcess()
                .stream()
                .map(this::process)
                .filter(Boolean::valueOf)
                .findFirst()
                .orElse(false);
    }

    private boolean process(BlobInfo blobInfo) {
        var status = false;
        try {
            var blobClient  = blobInfo.getBlobClient();
            LOG.info("{} BlobProcessor::blob {}",this.getClass().getName(),
                    blobClient.getBlobName());

            var printResponse = blobBackup.backupBlobs(blobInfo);
            var deleteBlob = blobStitch.stitchBlobs(printResponse);
            var leaseId = blobInfo.getLeaseId()
                    .orElseThrow(() ->
                            new LeaseIdNotPresentException("Lease not present"));
            blobClient.deleteWithResponse(
                    DeleteSnapshotsOptionType.INCLUDE,
                    new BlobRequestConditions().setLeaseId(leaseId),
                    null,
                    Context.NONE);
            status = blobDelete.deleteOriginalBlobs(deleteBlob);
            LOG.info("BlobProcessor:: delete original blobs");
        } catch (Exception e) {
            throw new BlobProcessException(String.format("Exception processing blob %s",
                    blobInfo.getBlobClient().getBlobName()), e);
        }
        return status;
    }
}
