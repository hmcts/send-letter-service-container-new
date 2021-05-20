package uk.gov.hmcts.reform.sendletter.blob;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.specialized.BlobLeaseClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.io.Resources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sendletter.blob.component.BlobBackup;
import uk.gov.hmcts.reform.sendletter.blob.component.BlobDelete;
import uk.gov.hmcts.reform.sendletter.blob.component.BlobManager;
import uk.gov.hmcts.reform.sendletter.blob.component.BlobReader;
import uk.gov.hmcts.reform.sendletter.blob.component.BlobStitch;
import uk.gov.hmcts.reform.sendletter.blob.storage.LeaseClientProvider;
import uk.gov.hmcts.reform.sendletter.model.in.DeleteBlob;
import uk.gov.hmcts.reform.sendletter.model.in.ManifestBlobInfo;
import uk.gov.hmcts.reform.sendletter.model.in.PrintResponse;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.io.Resources.getResource;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class BlobProcessorTest {
    private BlobProcessor processBlob;
    @Mock
    private BlobManager blobManager;
    @Mock
    private BlobReader blobReader;
    @Mock
    private BlobBackup blobBackup;
    @Mock
    private BlobStitch blobStitch;
    @Mock
    private BlobDelete blobDelete;

    private ManifestBlobInfo blobInfo;
    @Mock
    private BlobClient blobClient;
    @Mock
    private BlobContainerClient blobContainerClient;
    @Mock
    private LeaseClientProvider leaseClientProvider;
    @Mock
    private BlobLeaseClient blobLeaseClient;
    @Mock
    private BlobProperties properties;

    @BeforeEach
    void setUp() throws IOException {
        blobInfo = new ManifestBlobInfo("sscs", "new-sscs",
                "manifests-xyz.json");
        processBlob = new BlobProcessor(blobReader, blobBackup, blobStitch, blobDelete,
                blobManager, leaseClientProvider, 10);
        given(blobReader.retrieveManifestsToProcess()).willReturn(Optional.of(blobInfo));

        given(blobManager.getContainerClient(any())).willReturn(blobContainerClient);
        given(blobContainerClient.getBlobClient(any())).willReturn(blobClient);
        given(leaseClientProvider.get(blobClient)).willReturn(blobLeaseClient);
        given(blobClient.getProperties()).willReturn(properties);

        String json = Resources.toString(getResource("print_job_response.json"), UTF_8);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        PrintResponse printResponse = objectMapper.readValue(json, PrintResponse.class);

        given(blobBackup.backupBlobs(blobInfo)).willReturn(printResponse);

        DeleteBlob deleteBlob = new DeleteBlob();
        deleteBlob.setBlobName(List.of("33dffc2f-94e0-4584-a973-cc56849ecc0b-sscs-SSC001-mypdf.pdf",
                "33dffc2f-94e0-4584-a973-cc56849ecc0b-sscs-SSC001-1.pdf",
                "manifest-/manifest-33dffc2f-94e0-4584-a973-cc56849ecc0b-sscs.json"));
        deleteBlob.setContainerName("new-sscs");
        deleteBlob.setServiceName("sscs");
        given(blobStitch.stitchBlobs(printResponse)).willReturn(deleteBlob);

    }

    @Test
    void should_process_blob_when_triggered() throws IOException {
        boolean processed = processBlob.read();
        assertTrue(processed);

        var manifestBlobInfo = blobReader.retrieveManifestsToProcess();
        assertTrue(manifestBlobInfo.isPresent());

        PrintResponse printResponse = blobBackup.backupBlobs(blobInfo);
        assertNotNull(printResponse);

        //DeleteBlob deleteBlob = blobStitch.stitchBlobs(printResponse);
        //verify(blobDelete).deleteOriginalBlobs(deleteBlob);
    }
}