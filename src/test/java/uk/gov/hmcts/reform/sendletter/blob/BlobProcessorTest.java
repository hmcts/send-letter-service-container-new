package uk.gov.hmcts.reform.sendletter.blob;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobStorageException;
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
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

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

    @BeforeEach
    void setUp() {
        blobInfo = new ManifestBlobInfo("sscs", "new-sscs",
                "manifests-xyz.json");
        processBlob = new BlobProcessor(blobReader, blobBackup, blobStitch, blobDelete,
                blobManager, leaseClientProvider, 10);

    }

    @Test
    void should_process_blob_when_triggered() throws IOException {
        given(blobReader.retrieveManifestsToProcess()).willReturn(Optional.of(blobInfo));
        given(blobManager.getContainerClient(any())).willReturn(blobContainerClient);
        given(blobContainerClient.getBlobClient(any())).willReturn(blobClient);
        given(leaseClientProvider.get(blobClient)).willReturn(blobLeaseClient);
        var json = Resources.toString(getResource("print_job_response.json"), UTF_8);
        var objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        PrintResponse printResponse = objectMapper.readValue(json, PrintResponse.class);

        given(blobBackup.backupBlobs(blobInfo)).willReturn(printResponse);

        var deleteBlob = new DeleteBlob();
        deleteBlob.setBlobName(List.of("33dffc2f-94e0-4584-a973-cc56849ecc0b-sscs-SSC001-mypdf.pdf",
                "33dffc2f-94e0-4584-a973-cc56849ecc0b-sscs-SSC001-1.pdf"));
        deleteBlob.setContainerName("new-sscs");
        deleteBlob.setServiceName("sscs");
        given(blobStitch.stitchBlobs(printResponse)).willReturn(deleteBlob);

        boolean processed = processBlob.read();
        assertTrue(processed);

        var manifestBlobInfo = blobReader.retrieveManifestsToProcess();
        assertTrue(manifestBlobInfo.isPresent());

        var response = blobBackup.backupBlobs(blobInfo);
        assertNotNull(response);

        verify(blobDelete).deleteOriginalBlobs(blobStitch.stitchBlobs(response));

        verify(blobClient).deleteWithResponse(any(), any(), any(), any());
    }

    @Test
    void should_not_triggered_when_no_matching_blob_available() throws IOException {
        given(blobReader.retrieveManifestsToProcess()).willReturn(Optional.empty());
        assertFalse(processBlob.read());
    }

    @Test
    void should_throw_exception()  {
        var blobStorageException = new BlobStorageException("There is already a lease present for blob", null, null);
        given(blobLeaseClient.acquireLease(anyInt())).willThrow(blobStorageException);
        assertThatThrownBy(() -> blobLeaseClient.acquireLease(anyInt()))
                .isInstanceOf(BlobStorageException.class)
                .hasMessageContaining("There is already a lease present for blob");
    }

}