package uk.gov.hmcts.reform.sendletter.blob;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class BlobProcessorTest {
    private BlobProcessor processBlob;

    @BeforeEach
    void setUp() {
        processBlob = new BlobProcessor();
    }

    @Test
    void shouldProcessBlobWhenTriggered() {
        boolean processed = processBlob.read();
        assertThat(processed).isTrue();
    }

}