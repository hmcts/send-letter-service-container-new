package uk.gov.hmcts.reform.sendletter.blob;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sendletter.blob.component.BlobBackup;
import uk.gov.hmcts.reform.sendletter.blob.component.BlobReader;
import uk.gov.hmcts.reform.sendletter.model.in.ManifestBlobInfo;

import java.util.Optional;

@Service
public class BlobProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(BlobProcessor.class);

    private final BlobReader blobReader;
    private final BlobBackup blobBackup;

    public BlobProcessor(BlobReader blobReader, BlobBackup blobBackup) {
        this.blobReader = blobReader;
        this.blobBackup = blobBackup;
    }

    public boolean read() throws InterruptedException {
        Optional<ManifestBlobInfo> blobInfo = blobReader.retrieveManifestsToProcess();

        if (blobInfo.isPresent()) {
            var printResponse = blobBackup.backupBlobs(blobInfo.get());
            LOG.info("PrintResponse {}", printResponse);
        }

        Thread.sleep(10_000);
        return true;
    }
}
