package uk.gov.hmcts.reform.sendletter.blob;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.sendletter.blob.component.BlobReader;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BlobProcessorTest {
    private BlobProcessor processBlob;
    private BlobReader blobReader;

    @BeforeEach
    void setUp() {
        processBlob = new BlobProcessor(blobReader);
        when(blobReader.retrieveFileToProcess()).thenReturn("blob.txt");
    }

    @Test
    void should_process_blob_when_triggered() throws InterruptedException {
        boolean processed = processBlob.read();
        verify(blobReader).retrieveFileToProcess();
        assertThat(processed).isTrue();
    }
}