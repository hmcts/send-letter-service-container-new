package uk.gov.hmcts.reform.sendletter.blob;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sendletter.blob.component.BlobBackup;
import uk.gov.hmcts.reform.sendletter.blob.component.BlobReader;
import uk.gov.hmcts.reform.sendletter.blob.component.BlobStitch;
import uk.gov.hmcts.reform.sendletter.model.in.ManifestBlobInfo;

import java.io.IOException;
import java.util.Optional;

@Service
public class BlobProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(BlobProcessor.class);

    private final BlobReader blobReader;
    private final BlobBackup blobBackup;
    private final BlobStitch blobStitch;

    public BlobProcessor(BlobReader blobReader, BlobBackup blobBackup, BlobStitch blobStitch) {
        this.blobReader = blobReader;
        this.blobBackup = blobBackup;
        this.blobStitch = blobStitch;
    }

    public boolean read() throws IOException {
        Optional<ManifestBlobInfo> blobInfo = blobReader.retrieveManifestsToProcess();

        if (blobInfo.isPresent()) {
            var printResponse = blobBackup.backupBlobs(blobInfo.get());
            LOG.info("BlobProcessor:: PrintResponse {}", printResponse);
            blobStitch.stitchBlobs(printResponse);
        }

        return true;
    }
}
