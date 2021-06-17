package uk.gov.hmcts.reform.sendletter.blob;

import com.azure.storage.blob.BlobClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;
import uk.gov.hmcts.reform.sendletter.blob.component.BlobBackup2;
import uk.gov.hmcts.reform.sendletter.blob.component.BlobDelete;
import uk.gov.hmcts.reform.sendletter.blob.component.BlobManager;
import uk.gov.hmcts.reform.sendletter.blob.component.BlobReader2;
import uk.gov.hmcts.reform.sendletter.blob.component.BlobStitch;
import uk.gov.hmcts.reform.sendletter.model.in.BlobInfo;
import uk.gov.hmcts.reform.sendletter.model.in.DeleteBlob;
import uk.gov.hmcts.reform.sendletter.model.in.PrintResponse;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Charsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BlobProcessor2Test {
    private static final String TEST_NEW_CONTAINER = "new-bulkprint";
    private static final String TEST_NEW_SERVICE = "send_letter_tests";
    private static final String TEST_BLOB_NAME = "manifest-/print_job_response.json";
    private static final String TEST_PDF_1 = "33dffc2f-94e0-4584-a973-cc56849ecc0b-sscs-SSC001-mypdf.pdf";
    private static final String TEST_PDF_2 = "33dffc2f-94e0-4584-a973-cc56849ecc0b-sscs-SSC001-1.pdf";

    private BlobProcessor2 blobProcessor;
    @Mock
    private BlobManager blobManager;
    @Mock
    private BlobReader2 blobReader;
    @Mock
    private BlobBackup2 blobBackup;
    @Mock
    private BlobStitch blobStitch;
    @Mock
    private BlobDelete blobDelete;

    @Mock
    private BlobClient blobClient;
    private BlobInfo blobInfo;


    @BeforeEach
    void setUp() {

        blobInfo = new BlobInfo(blobClient);
        blobInfo.setLeaseId("LEASE_ID");
        blobInfo.setContainerName(TEST_NEW_CONTAINER);
        blobInfo.setServiceName(TEST_NEW_SERVICE);

        blobProcessor = new BlobProcessor2(blobReader, blobBackup, blobStitch, blobDelete);

        given(blobReader.retrieveBlobToProcess())
                .willReturn(Optional.of(blobInfo));
    }

    @Test
    void should_process_blob_when_triggered() throws IOException {
        var objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        var json = StreamUtils.copyToString(
                new ClassPathResource("print_job_response.json").getInputStream(), UTF_8);
        var printResponse = objectMapper.readValue(json, PrintResponse.class);
        given(blobClient.getBlobName()).willReturn(TEST_BLOB_NAME);
        given(blobBackup.backupBlobs(blobInfo)).willReturn(printResponse);

        var deleteBlob = new DeleteBlob();
        deleteBlob.setBlobName(List.of(TEST_PDF_1, TEST_PDF_2));
        deleteBlob.setContainerName(TEST_NEW_CONTAINER);
        deleteBlob.setServiceName(TEST_NEW_SERVICE);
        given(blobStitch.stitchBlobs(printResponse)).willReturn(deleteBlob);
        given(blobDelete.deleteOriginalBlobs(deleteBlob)).willReturn(true);

        boolean processed = blobProcessor.read();
        assertTrue(processed);

        verify(blobBackup).backupBlobs(blobInfo);
        verify(blobStitch).stitchBlobs(printResponse);
        verify(blobClient).deleteWithResponse(any(), any(), any(), any());
        verify(blobDelete).deleteOriginalBlobs(deleteBlob);
    }

    @Test
    void should_not_triggered_when_no_matching_blob_available()  {
        given(blobReader.retrieveBlobToProcess())
                .willReturn(Optional.empty());
        blobProcessor.read();
        verify(blobManager, never()).getContainerClient(anyString());
    }
}