package uk.gov.hmcts.reform.sendletter.blob.component;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sendletter.config.AccessTokenProperties;
import uk.gov.hmcts.reform.sendletter.model.in.DeleteBlob;
import uk.gov.hmcts.reform.sendletter.services.SasTokenGeneratorService;

import java.util.List;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class BlobDeleteTest {

    private static final String NEW_CONTAINER = "new-sscs";
    private static final String TEST_SERVICE_NAME = "sscs";

    private AccessTokenProperties accessTokenProperties;
    @Mock
    private BlobManager blobManager;
    @Mock
    private BlobClient client;

    private BlobDelete blobDelete;

    @BeforeEach
    void setUp() {

        StorageSharedKeyCredential storageCredentials =
                new StorageSharedKeyCredential("testAccountName", "dGVzdGtleQ==");

        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                .credential(storageCredentials)
                .endpoint("http://test.account")
                .buildClient();

        createAccessTokenConfig();

        var sasTokenGeneratorService = new SasTokenGeneratorService(blobServiceClient,
                accessTokenProperties);
        blobDelete = new BlobDelete(blobManager, sasTokenGeneratorService);
        var sasToken = sasTokenGeneratorService.generateSasToken(TEST_SERVICE_NAME);
        given(blobManager.getBlobClient(any(), any(), any())).willReturn(client);
    }

    @Test
    void should_delete_original_blob_with_path_from_new_container() {
        DeleteBlob deleteBlob = new DeleteBlob();
        deleteBlob.setBlobName(List.of("33dffc2f-94e0-4584-a973-cc56849ecc0b-sscs-SSC001-mypdf.pdf",
                "33dffc2f-94e0-4584-a973-cc56849ecc0b-sscs-SSC001-1.pdf",
                "manifest-/manifest-33dffc2f-94e0-4584-a973-cc56849ecc0b-sscs.json"));
        deleteBlob.setContainerName(NEW_CONTAINER);
        deleteBlob.setServiceName(TEST_SERVICE_NAME);
        Boolean response = blobDelete.deleteOriginalBlobs(deleteBlob);
        assertThat(response.booleanValue()).isSameAs(true);
    }

    private void createAccessTokenConfig() {
        AccessTokenProperties.TokenConfig tokenConfig = new AccessTokenProperties.TokenConfig();
        tokenConfig.setValidity(300);
        tokenConfig.setServiceName("sscs");

        accessTokenProperties = new AccessTokenProperties();
        accessTokenProperties.setServiceConfig(singletonList(tokenConfig));
    }
}