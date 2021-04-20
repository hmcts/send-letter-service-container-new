package uk.gov.hmcts.reform.sendletter.blob;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class BlobProcessorTest {
    private BlobProcessor processBlob;

    @BeforeEach
    public void setUp() {
        processBlob = new BlobProcessor();
    }

    @Test
    public void shouldProcessBlobWhenTriggered() {
        boolean processed = processBlob.read();
        assertThat(processed).isTrue();
    }

}