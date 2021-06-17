package uk.gov.hmcts.reform.sendletter.blob.component;

import com.azure.storage.blob.BlobClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sendletter.model.in.DeleteBlob;
import uk.gov.hmcts.reform.sendletter.services.SasTokenGeneratorService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class BlobDeleteTest {
    private static final String TEST_NEW_CONTAINER = "new-bulkprint";
    private static final String TEST_NEW_SERVICE = "send_letter_tests";

    @Mock
    private SasTokenGeneratorService sasTokenGeneratorService;
    @Mock
    private BlobManager blobManager;
    @Mock
    private BlobClient client;

    private BlobDelete blobDelete;

    @BeforeEach
    void setUp() {
        blobDelete = new BlobDelete(blobManager, sasTokenGeneratorService);
        given(sasTokenGeneratorService.generateSasToken(TEST_NEW_SERVICE))
                .willReturn("sasToken");
        given(blobManager.getBlobClient(any(), any(), any())).willReturn(client);
    }

    @Test
    void should_delete_original_blob_with_path_from_new_container() {
        var deleteBlob = new DeleteBlob();
        deleteBlob.setBlobName(List.of("33dffc2f-94e0-4584-a973-cc56849ecc0b-sscs-SSC001-mypdf.pdf",
                "33dffc2f-94e0-4584-a973-cc56849ecc0b-sscs-SSC001-1.pdf",
                "manifest-/manifest-33dffc2f-94e0-4584-a973-cc56849ecc0b-sscs.json"));
        deleteBlob.setContainerName(TEST_NEW_CONTAINER);
        deleteBlob.setServiceName(TEST_NEW_SERVICE);
        boolean response = blobDelete.deleteOriginalBlobs(deleteBlob);
        assertThat(response).isSameAs(true);
    }
}