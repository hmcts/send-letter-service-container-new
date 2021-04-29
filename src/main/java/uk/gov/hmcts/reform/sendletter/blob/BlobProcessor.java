package uk.gov.hmcts.reform.sendletter.blob;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sendletter.blob.component.BlobReader;

@Service
public class BlobProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(BlobProcessor.class);
    private final BlobReader blobReader;

    public BlobProcessor(BlobReader blobReader) {
        this.blobReader = blobReader;
    }

    public boolean read() throws InterruptedException {
        String blob = blobReader.retrieveFileToProcess();
        LOG.info("BlobName : {}", blob);
        Thread.sleep(10_000);
        return true;
    }
}
