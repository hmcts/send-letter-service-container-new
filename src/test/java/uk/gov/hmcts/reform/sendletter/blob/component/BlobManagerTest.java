package uk.gov.hmcts.reform.sendletter.blob.component;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sendletter.config.AccessTokenProperties;
import uk.gov.hmcts.reform.sendletter.services.SasTokenGeneratorService;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BlobManagerTest {

    private static final String NEW_CONTAINER = "new-sscs";
    private static final String BLOB_NAME  = "manifests-xyz.json";

    @Mock
    private BlobServiceClient blobServiceClient;

    private String sasToken;
    private BlobManager blobManager;
    private AccessTokenProperties accessTokenProperties;

    @BeforeEach
    void setUp() {

        blobManager = new BlobManager(blobServiceClient);

        StorageSharedKeyCredential storageCredentials =
                new StorageSharedKeyCredential("testAccountName", "dGVzdGtleQ==");

        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                .credential(storageCredentials)
                .endpoint("http://test.account")
                .buildClient();

        createAccessTokenConfig();

        SasTokenGeneratorService tokenGeneratorService =
                new SasTokenGeneratorService(blobServiceClient, accessTokenProperties);
        sasToken = tokenGeneratorService.generateSasToken("sscs");
    }

    @Test
    void retrieves_container_from_client() {
        BlobContainerClient expectedContainer = mock(BlobContainerClient.class);
        String containerName = "container-name";

        given(blobServiceClient.getBlobContainerClient(any())).willReturn(expectedContainer);
        BlobContainerClient actualContainer = blobManager.getContainerClient(containerName);

        assertThat(actualContainer).isSameAs(expectedContainer);
        verify(blobServiceClient).getBlobContainerClient(containerName);
    }

    @Test
    void retrieves_blob_client() {
        given(blobManager.getAccountUrl()).willReturn("http://test.account");
        BlobClient actualClient = blobManager.getBlobClient(NEW_CONTAINER, sasToken, BLOB_NAME);
        assertNotNull(actualClient);
        assertThat(actualClient).isInstanceOf(BlobClient.class);
    }

    @Test
    void retrieves_account_url() {
        given(blobManager.getAccountUrl()).willReturn("http://test.account");
        String accountUrl = blobManager.getAccountUrl();
        assertThat(accountUrl).isSameAs("http://test.account");
    }

    private void createAccessTokenConfig() {
        AccessTokenProperties.TokenConfig tokenConfig = new AccessTokenProperties.TokenConfig();
        tokenConfig.setValidity(300);
        tokenConfig.setServiceName("sscs");

        accessTokenProperties = new AccessTokenProperties();
        accessTokenProperties.setServiceConfig(singletonList(tokenConfig));
    }
}
