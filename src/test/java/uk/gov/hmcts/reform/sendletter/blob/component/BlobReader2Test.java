package uk.gov.hmcts.reform.sendletter.blob.component;

import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.specialized.BlobLeaseClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sendletter.blob.storage.LeaseClientProvider;
import uk.gov.hmcts.reform.sendletter.config.AccessTokenProperties;
import uk.gov.hmcts.reform.sendletter.config.AccessTokenProperties.TokenConfig;

import java.util.List;
import java.util.function.BiFunction;

import static java.util.List.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BlobReader2Test {
    @Mock
    private BlobManager blobManager;
    @Mock
    private LeaseClientProvider leaseClientProvider;
    private AccessTokenProperties accessTokenProperties;
    @Mock
    private BlobContainerClient blobContainerClient;
    @Mock
    private PagedIterable<BlobItem> mockedPagedIterable;
    @Mock
    private BlobItem mockedBlobItemFirst;
    @Mock
    private BlobItem mockedBlobItemSecond;
    @Mock
    private BlobItem mockedBlobItemThird;
    @Mock
    private BlobLeaseClient blobLeaseClient;
    @Mock
    private BlobClient blobClient;

    private BlobReader2 blobReader;

    @BeforeEach
    void setUp() {
        createAccessTokenConfig();
        blobReader = new BlobReader2(
                blobManager,
                accessTokenProperties,
                leaseClientProvider,
                30
        );
    }

    @Test
    void should_return_leased_blob_info_when_lease_acquired() {
        given(blobManager.getContainerClient("new-bulkprint"))
                .willReturn(blobContainerClient);
        given(mockedBlobItemFirst.getName()).willReturn("manifests-xyz.json");
        given(mockedBlobItemSecond.getName()).willReturn("manifests-abc.json");
        given(mockedBlobItemThird.getName()).willReturn("manifests-lmn.json");
        var blobItems = List.of(
                mockedBlobItemFirst,
                mockedBlobItemSecond,
                mockedBlobItemThird);
        given(mockedPagedIterable.stream()).willReturn(blobItems.stream());
        given(blobContainerClient.listBlobs()).willReturn(mockedPagedIterable);

        given(leaseClientProvider.get(blobClient)).willReturn(blobLeaseClient);
        given(blobContainerClient.getBlobClient(anyString())).willReturn(blobClient);
        String leasedId = "leased";
        given(blobLeaseClient.acquireLease(anyInt()))
                .willThrow(new RuntimeException("First already leased"))
                .willThrow(new RuntimeException("Second already leased"))
                .willReturn(leasedId);

        var mayBeBlobInfo = blobReader.retrieveBlobToProcess();
        assertThat(mayBeBlobInfo).isPresent();

        var blobInfo = mayBeBlobInfo.get();
        assertThat(blobInfo.isLeased()).isTrue();
        assertThat(blobInfo.getServiceName()).isEqualTo("send_letter_tests");
        assertThat(blobInfo.getContainerName()).isEqualTo("new-bulkprint");

        verify(blobManager).getContainerClient("new-bulkprint");
        verify(blobContainerClient, times(3))
                .getBlobClient(anyString());
        verify(leaseClientProvider, times(3))
                .get(blobClient);
        verify(blobLeaseClient, times(3))
                .acquireLease(30);
    }

    private void createAccessTokenConfig() {
        BiFunction<String, String, TokenConfig> tokenFunction = (service, container) -> {
            AccessTokenProperties.TokenConfig tokenConfig = new AccessTokenProperties.TokenConfig();
            tokenConfig.setValidity(300);
            tokenConfig.setServiceName(service);
            tokenConfig.setContainerName(container);
            return tokenConfig;
        };
        accessTokenProperties = new AccessTokenProperties();
        accessTokenProperties.setServiceConfig(
                of(
                    tokenFunction.apply("send_letter_tests", "new-bulkprint"),
                    tokenFunction.apply("send_letter_backup", "backup"),
                    tokenFunction.apply("send_letter_process", "processed")
                )
        );
    }
}