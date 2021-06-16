package uk.gov.hmcts.reform.sendletter.blob;

import com.azure.core.util.Context;
import com.azure.storage.blob.models.BlobRequestConditions;
import com.azure.storage.blob.models.DeleteSnapshotsOptionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sendletter.blob.component.BlobBackup2;
import uk.gov.hmcts.reform.sendletter.blob.component.BlobDelete;
import uk.gov.hmcts.reform.sendletter.blob.component.BlobReader2;
import uk.gov.hmcts.reform.sendletter.blob.component.BlobStitch;
import uk.gov.hmcts.reform.sendletter.exceptions.LeaseIdNotPresentException;
import uk.gov.hmcts.reform.sendletter.model.in.BlobInfo;

@Service
public class BlobProcessor2 {
    private static final Logger LOG = LoggerFactory.getLogger(BlobProcessor2.class);

    private final BlobReader2 blobReader;
    private final BlobBackup2 blobBackup;
    private final BlobStitch blobStitch;
    private final BlobDelete blobDelete;
    private final Integer leaseTime;

    public BlobProcessor2(BlobReader2 blobReader, BlobBackup2 blobBackup, BlobStitch blobStitch,
                         BlobDelete blobDelete,
                         @Value("${storage.leaseTime}") Integer leaseTime) {
        this.blobReader = blobReader;
        this.blobBackup = blobBackup;
        this.blobStitch = blobStitch;
        this.blobDelete = blobDelete;
        this.leaseTime = leaseTime;
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
            LOG.info("BlobProcessor:: backup blobs response {}", printResponse);
            var deleteBlob = blobStitch.stitchBlobs(printResponse);
            String leaseId = blobInfo.getLeaseId()
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
            LOG.error("Exception processing blob {}", blobInfo.getBlobClient().getBlobName(), e);
        }
        return status;
    }
}
