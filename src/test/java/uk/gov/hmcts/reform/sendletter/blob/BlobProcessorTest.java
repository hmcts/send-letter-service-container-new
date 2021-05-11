package uk.gov.hmcts.reform.sendletter.blob;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.io.Resources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sendletter.blob.component.BlobBackup;
import uk.gov.hmcts.reform.sendletter.blob.component.BlobReader;
import uk.gov.hmcts.reform.sendletter.model.in.ManifestBlobInfo;
import uk.gov.hmcts.reform.sendletter.model.out.PrintResponse;

import java.io.IOException;
import java.util.Optional;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.io.Resources.getResource;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BlobProcessorTest {
    private BlobProcessor processBlob;

    @Mock
    private BlobReader blobReader;
    @Mock
    private BlobBackup blobBackup;

    private ManifestBlobInfo blobInfo;

    @BeforeEach
    void setUp() throws IOException {
        blobInfo = new ManifestBlobInfo("sscs", "new-sscs",
                "manifests-xyz.json");
        processBlob = new BlobProcessor(blobReader, blobBackup);
        given(blobReader.retrieveManifestsToProcess()).willReturn(Optional.of(blobInfo));

        String json = Resources.toString(getResource("print_job_response.json"), UTF_8);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        PrintResponse printResponse = objectMapper.readValue(json, PrintResponse.class);

        given(blobBackup.backupBlobs(blobInfo)).willReturn(printResponse);
    }

    @Test
    void should_process_blob_when_triggered() throws InterruptedException {
        boolean processed = processBlob.read();
        verify(blobReader).retrieveManifestsToProcess();
        PrintResponse printResponse = blobBackup.backupBlobs(blobInfo);

        assertThat(processed).isTrue();
        assertThat(printResponse.printUploadInfo.manifestPath)
                .isEqualTo("manifest-33dffc2f-94e0-4584-a973-cc56849ecc0b-sscs.json");
    }
}