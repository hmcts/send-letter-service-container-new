package uk.gov.hmcts.reform.sendletter.blob.component;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class BlobBackupTest {

    private static final String NEW_CONTAINER = "new";
    private static final String BACKUP_CONTAINER = "backup";

    @Mock private BlobManager blobManager;
    @Mock private BlobContainerClient sourceContainerClient;
    @Mock private BlobContainerClient destContainerClient;
    @Mock private BlobClient sourceBlobClient;
    @Mock private BlobClient destBlobClient;

    private BlobBackup blobBackup;

    @BeforeEach
    void setUp() {
        blobBackup = new BlobBackup(blobManager);
    }

    @Test
    void should_copy_blob_from_new_container_to_backup_container() {
        String originalBlob  = "blob.zip";
        given(blobManager.listContainerClient(NEW_CONTAINER)).willReturn(sourceContainerClient);
        given(blobManager.listContainerClient(BACKUP_CONTAINER)).willReturn(destContainerClient);
        given(sourceContainerClient.getBlobClient(anyString())).willReturn(sourceBlobClient);
        given(destContainerClient.getBlobClient(anyString())).willReturn(destBlobClient);
        given(destBlobClient.copyFromUrl(sourceBlobClient.getBlobUrl())).willReturn("copyId");

        assertThat(blobBackup.backupBlob(originalBlob)).isEqualTo("copyId");
    }
}