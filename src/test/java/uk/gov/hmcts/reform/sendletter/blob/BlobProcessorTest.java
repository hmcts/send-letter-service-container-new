package uk.gov.hmcts.reform.sendletter.blob;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobRequestConditions;
import com.azure.storage.blob.specialized.BlobLeaseClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.io.Resources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.io.Resources.getResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BlobProcessorTest {
    private static final String CONTAINER = "new-sscs";
    private static final String SERVICE = "sscs";

    private BlobProcessor blobProcessor;
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

    private List<ManifestBlobInfo> blobInfos;
    @Mock
    private BlobClient blobClient;
    @Mock
    private BlobContainerClient blobContainerClient;
    @Mock
    private LeaseClientProvider leaseClientProvider;
    @Mock
    private BlobLeaseClient blobLeaseClient;

    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        objectMapper.registerModule(new JavaTimeModule());
        blobInfos = List.of(
                new ManifestBlobInfo(SERVICE, CONTAINER,
                "manifests-xyz.json"),
                new ManifestBlobInfo(SERVICE, CONTAINER,
                "manifests-abc.json"),
                new ManifestBlobInfo(SERVICE, CONTAINER,
                "manifests-lmn.json")
        );

        blobProcessor = new BlobProcessor(blobReader, blobBackup, blobStitch, blobDelete,
                blobManager, leaseClientProvider, 10);
        given(blobReader.retrieveManifestsToProcess())
                .willReturn(blobInfos);
    }

    @Test
    void should_process_blob_when_triggered() throws IOException {
        given(blobManager.getContainerClient(any())).willReturn(blobContainerClient);
        given(blobContainerClient.getBlobClient(any())).willReturn(blobClient);
        given(leaseClientProvider.get(blobClient)).willReturn(blobLeaseClient);
        var json = Resources.toString(getResource("print_job_response.json"), UTF_8);

        PrintResponse printResponse = objectMapper.readValue(json, PrintResponse.class);

        given(blobBackup.backupBlobs(blobInfos.get(0))).willReturn(printResponse);

        var deleteBlob = new DeleteBlob();
        deleteBlob.setBlobName(List.of("33dffc2f-94e0-4584-a973-cc56849ecc0b-sscs-SSC001-mypdf.pdf",
                "33dffc2f-94e0-4584-a973-cc56849ecc0b-sscs-SSC001-1.pdf"));
        deleteBlob.setContainerName(CONTAINER);
        deleteBlob.setServiceName(SERVICE);
        given(blobStitch.stitchBlobs(printResponse)).willReturn(deleteBlob);
        given(blobDelete.deleteOriginalBlobs(deleteBlob)).willReturn(true);

        boolean processed = blobProcessor.read();
        assertTrue(processed);

        ManifestBlobInfo firstManifestBlobInfo = blobInfos.get(0);
        verify(blobManager).getContainerClient(firstManifestBlobInfo.getContainerName());
        verify(blobLeaseClient).acquireLease(anyInt());
        verify(blobBackup).backupBlobs(firstManifestBlobInfo);
        verify(blobStitch).stitchBlobs(printResponse);
        verify(blobClient).deleteWithResponse(any(), any(), any(), any());
        verify(blobDelete).deleteOriginalBlobs(deleteBlob);
    }

    @Test
    void should_not_triggered_when_no_matching_blob_available() throws IOException {
        given(blobReader.retrieveManifestsToProcess())
                .willReturn(Collections.emptyList());
        blobProcessor.read();
        verify(blobManager, never()).getContainerClient(anyString());
    }

    @Test
    void should_process_second_manisfest_file_when_first_two_are_leased() throws IOException {
        given(blobManager.getContainerClient(any())).willReturn(blobContainerClient);
        given(blobContainerClient.getBlobClient(any())).willReturn(blobClient);
        given(leaseClientProvider.get(blobClient)).willReturn(blobLeaseClient);
        String leasedId = "leased";
        given(blobLeaseClient.acquireLease(anyInt()))
                .willThrow(new RuntimeException("First already leased"))
                .willThrow(new RuntimeException("Second already leased"))
                .willReturn(leasedId);

        var json = Resources.toString(getResource("print_job_response.json"), UTF_8);

        PrintResponse printResponse = objectMapper.readValue(json, PrintResponse.class);

        given(blobBackup.backupBlobs(isA(ManifestBlobInfo.class))).willReturn(printResponse);

        var deleteBlob = new DeleteBlob();
        deleteBlob.setBlobName(List.of("33dffc2f-94e0-4584-a973-cc56849ecc0b-sscs-SSC001-mypdf.pdf",
                "33dffc2f-94e0-4584-a973-cc56849ecc0b-sscs-SSC001-1.pdf"));
        deleteBlob.setContainerName(CONTAINER);
        deleteBlob.setServiceName(SERVICE);
        given(blobStitch.stitchBlobs(printResponse)).willReturn(deleteBlob);
        given(blobDelete.deleteOriginalBlobs(deleteBlob)).willReturn(true);

        boolean processed = blobProcessor.read();
        assertTrue(processed);

        verify(blobManager, times(3)).getContainerClient(CONTAINER);
        verify(blobLeaseClient, times(3)).acquireLease(anyInt());
        verify(blobBackup, times(1)).backupBlobs(blobInfos.get(2));
        verify(blobStitch).stitchBlobs(printResponse);
        ArgumentCaptor<BlobRequestConditions> blobRequestConditionArg =
                ArgumentCaptor.forClass(BlobRequestConditions.class);
        verify(blobClient).deleteWithResponse(any(), blobRequestConditionArg.capture(), any(), any());
        assertThat(blobRequestConditionArg.getValue().getLeaseId())
                .isEqualTo(leasedId);
        verify(blobDelete).deleteOriginalBlobs(deleteBlob);
    }
}