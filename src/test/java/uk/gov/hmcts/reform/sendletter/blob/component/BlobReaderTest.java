package uk.gov.hmcts.reform.sendletter.blob.component;

import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sendletter.config.AccessTokenProperties;
import uk.gov.hmcts.reform.sendletter.model.in.ManifestBlobInfo;

import java.util.Optional;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class BlobReaderTest {

    private static final String CONTAINER_1 = "new-sscs";

    @Mock private BlobManager blobManager;
    @Mock private BlobContainerClient blobContainerClient;

    @Mock BlobItem mockedBlobItem;
    @Mock PagedIterable<BlobItem> mockedPagedIterable;

    private BlobReader blobReader;
    private AccessTokenProperties accessTokenProperties;

    @BeforeEach
    void setUp() {
        createAccessTokenConfig();
        blobReader = new BlobReader(blobManager, accessTokenProperties);
    }

    @Test
    void should_list_blob_name()  {
        given(blobManager.getContainerClient(CONTAINER_1)).willReturn(blobContainerClient);
        given(blobContainerClient.listBlobs()).willReturn(mockedPagedIterable);
        given(mockedBlobItem.getName()).willReturn("manifest-/manifests-xyz.json");
        given(mockedPagedIterable.stream().filter(m -> m.getName().startsWith("manifest")))
                .willReturn(Stream.of(mockedBlobItem));
        Optional<ManifestBlobInfo> manifestBlobInfo = blobReader.retrieveManifestsToProcess();

        assertThat(manifestBlobInfo.get().getBlobName()).isEqualTo("manifests-xyz.json");
        assertThat(manifestBlobInfo.get().getServiceName()).isEqualTo("sscs");
        assertThat(manifestBlobInfo.get().getContainerName()).isEqualTo("new-sscs");
    }

    private void createAccessTokenConfig() {
        AccessTokenProperties.TokenConfig tokenConfig = new AccessTokenProperties.TokenConfig();
        tokenConfig.setValidity(300);
        tokenConfig.setServiceName("sscs");
        tokenConfig.setNewContainerName(CONTAINER_1);
        accessTokenProperties = new AccessTokenProperties();
        accessTokenProperties.setServiceConfig(singletonList(tokenConfig));
    }
}