package uk.gov.hmcts.reform.sendletter.blob.component;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class BlobManagerTest {

    @Mock
    private BlobServiceClient blobServiceClient;

    private BlobManager blobManager;

    @BeforeEach
    public void setUp() {
        blobManager = new BlobManager(blobServiceClient);
    }

    @Test
    public void listContainer_retrieves_container_from_client() {
        BlobContainerClient expectedContainer = mock(BlobContainerClient.class);
        String containerName = "container-name";

        given(blobServiceClient.getBlobContainerClient(any())).willReturn(expectedContainer);
        BlobContainerClient actualContainer = blobManager.listContainerClient(containerName);

        assertThat(actualContainer).isSameAs(expectedContainer);
        verify(blobServiceClient).getBlobContainerClient(containerName);
    }
}
