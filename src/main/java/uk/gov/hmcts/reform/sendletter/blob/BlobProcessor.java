package uk.gov.hmcts.reform.sendletter.blob;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sendletter.blob.component.BlobBackup;
import uk.gov.hmcts.reform.sendletter.blob.component.BlobReader;

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
        String blob = blobReader.retrieveFileToProcess();
        blobBackup.backupBlob(blob);
        LOG.info("BlobName : {}", blob);
        Thread.sleep(10_000);
        return true;
    }
}
