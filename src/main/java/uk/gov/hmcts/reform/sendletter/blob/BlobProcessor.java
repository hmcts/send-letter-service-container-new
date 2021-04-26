package uk.gov.hmcts.reform.sendletter.blob;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class BlobProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(BlobProcessor.class);
    private final String connection;

    public BlobProcessor(@Value("${storage.connection}") String accountConnection) {
        this.connection = accountConnection;
    }

    public boolean read() throws InterruptedException {
        LOG.info("About to read new blob connection details");
        Thread.sleep(10_000);
        return true;
    }
}
