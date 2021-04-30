package uk.gov.hmcts.reform.sendletter.blob.component;

import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class BlobReaderTest {

    private static final String CONTAINER_1 = "new";

    @Mock private BlobManager blobManager;
    @Mock private BlobContainerClient blobContainerClient;

    @Mock BlobItem mockedBlobItem;
    @Mock PagedIterable<BlobItem> mockedPagedIterable;

    private BlobReader blobReader;

    @BeforeEach
    void setUp() {
        blobReader = new BlobReader(blobManager);
    }

    @Test
    void should_list_blob_name()  {
        given(blobManager.listContainerClient(CONTAINER_1)).willReturn(blobContainerClient);
        given(blobContainerClient.listBlobs()).willReturn(mockedPagedIterable);
        given(mockedBlobItem.getName()).willReturn("test.zip");
        given(mockedPagedIterable.stream()).willReturn(Stream.of(mockedBlobItem));
        String blob = blobReader.retrieveFileToProcess();
        assertThat(blob).isEqualTo("test.zip");
    }
}