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

import java.util.List;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class BlobReaderTest {

    private static final String CONTAINER = "new-sscs";
    private static final String SERVICE = "sscs";

    @Mock private BlobManager blobManager;
    @Mock private BlobContainerClient blobContainerClient;

    @Mock BlobItem mockedBlobItemFirst;
    @Mock BlobItem mockedBlobItemSecond;
    @Mock BlobItem mockedBlobItemThird;
    @Mock BlobItem mockedBlobItemFourth;
    @Mock BlobItem mockedBlobItemFifth;
    @Mock PagedIterable<BlobItem> mockedPagedIterable;
    private BlobReader blobReader;
    private AccessTokenProperties accessTokenProperties;

    @BeforeEach
    void setUp() {
        createAccessTokenConfig();
        blobReader = new BlobReader(blobManager, accessTokenProperties);
    }

    @Test
    void should_return_list_of_manifestblobsinfo_when_manifest_files_present()  {
        given(blobManager.getContainerClient(CONTAINER)).willReturn(blobContainerClient);
        given(blobContainerClient.listBlobs()).willReturn(mockedPagedIterable);
        given(mockedBlobItemFirst.getName()).willReturn("manifests-xyz.json");
        given(mockedBlobItemSecond.getName()).willReturn("xyz.json");
        given(mockedBlobItemThird.getName()).willReturn("manifests-lmn.json");
        given(mockedBlobItemFourth.getName()).willReturn("abc.json");
        given(mockedBlobItemFifth.getName()).willReturn("manifests-abc.json");
        var blobItems = List.of(
                mockedBlobItemFirst,
                mockedBlobItemSecond,
                mockedBlobItemThird,
                mockedBlobItemFourth,
                mockedBlobItemFifth);

        var stream = blobItems.stream();

        given(mockedPagedIterable.stream())
                .willReturn(stream);

        var manifestBlobsInfo = blobReader.retrieveManifestsToProcess();
        assertThat(manifestBlobsInfo)
                .as("Manifest info")
                .extracting("serviceName", "containerName", "blobName")
                .containsExactly(
                        tuple(SERVICE, CONTAINER, "manifests-xyz.json"),
                        tuple(SERVICE, CONTAINER, "manifests-lmn.json"),
                        tuple(SERVICE, CONTAINER, "manifests-abc.json")
            );
    }

    @Test
    void should_empty_list_when_no_manifest_file_present()  {
        given(blobManager.getContainerClient(CONTAINER)).willReturn(blobContainerClient);
        given(blobContainerClient.listBlobs()).willReturn(mockedPagedIterable);

        given(mockedBlobItemFirst.getName()).willReturn("xyz.json");
        given(mockedBlobItemSecond.getName()).willReturn("abc.json");
        given(mockedBlobItemThird.getName()).willReturn("lmn.json");
        var blobItems = List.of(
                mockedBlobItemFirst,
                mockedBlobItemSecond,
                mockedBlobItemThird);

        var stream = blobItems.stream();


        given(mockedPagedIterable.stream())
                .willReturn(stream);

        List<ManifestBlobInfo> manifestBlobsInfo = blobReader.retrieveManifestsToProcess();
        assertThat(manifestBlobsInfo).isEmpty();
    }

    private void createAccessTokenConfig() {
        AccessTokenProperties.TokenConfig tokenConfig = new AccessTokenProperties.TokenConfig();
        tokenConfig.setValidity(300);
        tokenConfig.setServiceName(SERVICE);
        tokenConfig.setNewContainerName(CONTAINER);
        accessTokenProperties = new AccessTokenProperties();
        accessTokenProperties.setServiceConfig(singletonList(tokenConfig));
    }
}