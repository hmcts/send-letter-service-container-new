package uk.gov.hmcts.reform.sendletter.blob;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class BlobProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(BlobProcessor.class);
    private final String connection;
    private final String azureKey;

    public BlobProcessor(@Value("${storage.connection}") String accountConnection,
                         @Value("${azure.keyValue}") String azureKey) {
        this.connection = accountConnection;
        this.azureKey = azureKey;
    }

    public boolean read() throws InterruptedException {
        LOG.info("About to read new blob connection details {}", connection);
        LOG.info("Azure key {}", azureKey);
        Thread.sleep(10_000);
        return true;
    }
}
