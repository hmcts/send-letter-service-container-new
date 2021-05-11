package uk.gov.hmcts.reform.sendletter.blob.component;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobClientBuilder;
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
import uk.gov.hmcts.reform.sendletter.model.in.ManifestBlobInfo;
import uk.gov.hmcts.reform.sendletter.services.SasTokenGeneratorService;

import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class BlobBackupTest {

    private static final String NEW_CONTAINER = "new-sscs";
    private static final String BACKUP_CONTAINER = "backup";
    private static final String BLOB_NAME  = "manifests-xyz.json";

    private AccessTokenProperties accessTokenProperties;

    @Mock private BlobManager blobManager;
    @Mock private BlobContainerClient destContainerClient;
    @Mock private BlobClient destBlobClient;

    private BlobClient sourceBlobClient;
    private BlobBackup blobBackup;
    private String sasToken;
    private ManifestBlobInfo blobInfo;

    @BeforeEach
    void setUp() {
        given(blobManager.getAccountUrl()).willReturn("http://test.account");

        StorageSharedKeyCredential storageCredentials =
                new StorageSharedKeyCredential("testAccountName", "dGVzdGtleQ==");

        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                .credential(storageCredentials)
                .endpoint("http://test.account")
                .buildClient();

        createAccessTokenConfig();

        SasTokenGeneratorService tokenGeneratorService =
                new SasTokenGeneratorService(blobServiceClient, accessTokenProperties);
        blobBackup = new BlobBackup(blobManager, tokenGeneratorService);

        sasToken = tokenGeneratorService.generateSasToken("sscs");

        sourceBlobClient = new BlobClientBuilder()
                .endpoint(blobManager.getAccountUrl())
                .sasToken(sasToken)
                .containerName(NEW_CONTAINER)
                .blobName(BLOB_NAME)
                .buildClient();

        blobInfo =  new ManifestBlobInfo("sscs", NEW_CONTAINER, BLOB_NAME);
    }

    @Test
    void should_copy_blob_from_new_container_to_backup_container() {
        given(blobManager.getContainerClient(BACKUP_CONTAINER)).willReturn(destContainerClient);
        given(destBlobClient.copyFromUrl(sourceBlobClient.getBlobUrl() + "?" + sasToken)).willReturn("copyId");
        given(destContainerClient.getBlobClient(anyString())).willReturn(destBlobClient);

        //blobBackup.backupBlobs(blobInfo);
    }


    private void createAccessTokenConfig() {
        AccessTokenProperties.TokenConfig tokenConfig = new AccessTokenProperties.TokenConfig();
        tokenConfig.setValidity(300);
        tokenConfig.setServiceName("sscs");

        accessTokenProperties = new AccessTokenProperties();
        accessTokenProperties.setServiceConfig(singletonList(tokenConfig));
    }
}