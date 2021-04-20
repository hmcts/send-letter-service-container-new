package uk.gov.hmcts.reform.sendletter.blob;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BlobProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlobProcessor.class);

    public boolean read() {
        LOGGER.info("Blob read");
        return true;
    }
}
