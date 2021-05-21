package uk.gov.hmcts.reform.sendletter.blob.component;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobClientBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.io.Resources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sendletter.config.AccessTokenProperties;
import uk.gov.hmcts.reform.sendletter.model.in.ManifestBlobInfo;
import uk.gov.hmcts.reform.sendletter.model.in.PrintResponse;
import uk.gov.hmcts.reform.sendletter.services.SasTokenGeneratorService;

import java.io.FileWriter;
import java.io.IOException;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.io.Resources.getResource;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BlobBackupTest {

    private static final String NEW_CONTAINER = "new-sscs";
    private static final String BACKUP_CONTAINER = "backup";
    private static final String BLOB_NAME = "manifest-/print_job_response.json";
    private static final String TEST_SERVICE_NAME = "sscs";

    private AccessTokenProperties accessTokenProperties;

    @Mock
    private BlobManager blobManager;
    @Mock
    private BlobContainerClient destContainerClient;
    @Mock
    private BlobClient destBlobClient;
    @Mock
    private BlobClient client;

    private BlobBackup blobBackup;
    private ManifestBlobInfo blobInfo;
    private String sasToken;
    private ObjectMapper mapper;
    private String json;

    @BeforeEach
    void setUp() throws IOException {
        mapper = new ObjectMapper();
        given(blobManager.getAccountUrl()).willReturn("http://test.account");


        StorageSharedKeyCredential storageCredentials =
                new StorageSharedKeyCredential("testAccountName", "dGVzdGtleQ==");

        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                .credential(storageCredentials)
                .endpoint("http://test.account")
                .buildClient();

        createAccessTokenConfig();

        SasTokenGeneratorService sasTokenGeneratorService = new SasTokenGeneratorService(blobServiceClient,
                accessTokenProperties);
        blobBackup = new BlobBackup(blobManager, sasTokenGeneratorService, mapper);

        sasToken = sasTokenGeneratorService.generateSasToken(TEST_SERVICE_NAME);

        var sourceBlobClient = new BlobClientBuilder()
                .endpoint(blobManager.getAccountUrl())
                .sasToken(sasToken)
                .containerName(NEW_CONTAINER)
                .blobName(BLOB_NAME)
                .buildClient();

        blobInfo = new ManifestBlobInfo(TEST_SERVICE_NAME, NEW_CONTAINER, BLOB_NAME);
        given(blobManager.getContainerClient(BACKUP_CONTAINER)).willReturn(destContainerClient);
        given(destContainerClient.getBlobClient(anyString())).willReturn(destBlobClient);
        given(blobManager.getBlobClient(any(), any(), any())).willReturn(client);

        mapper = spy(mapper);
        json = Resources.toString(getResource("print_job_response.json"), UTF_8);
        mapper.registerModule(new JavaTimeModule());
        PrintResponse printResponse = mock(PrintResponse.class);

        FileWriter myWriter = new FileWriter("/var/tmp/print_job_response.json");
        myWriter.write(json);
        myWriter.close();

        lenient().when(mapper.readValue(json, PrintResponse.class)).thenReturn(printResponse);
    }

    @Test
    void should_copy_blob_with_path_from_new_container_to_backup_container() {
        var response = blobBackup.backupBlobs(blobInfo);
        assertNotNull(response);
        verify(destBlobClient, times(3)).copyFromUrl(any());
    }

    @Test
    void should_copy_blob_without_path_from_new_container_to_backup_container() {
        String blobName = "print_job_response.json";
        var sourceBlobClient = new BlobClientBuilder()
                .endpoint(blobManager.getAccountUrl())
                .sasToken(sasToken)
                .containerName(NEW_CONTAINER)
                .blobName(blobName)
                .buildClient();

        blobInfo = new ManifestBlobInfo(TEST_SERVICE_NAME, NEW_CONTAINER, blobName);

        var response = blobBackup.backupBlobs(blobInfo);
        assertNotNull(response);
        verify(destBlobClient, times(3)).copyFromUrl(any());
    }

    private void createAccessTokenConfig() {
        AccessTokenProperties.TokenConfig tokenConfig = new AccessTokenProperties.TokenConfig();
        tokenConfig.setValidity(300);
        tokenConfig.setServiceName("sscs");

        accessTokenProperties = new AccessTokenProperties();
        accessTokenProperties.setServiceConfig(singletonList(tokenConfig));
    }
}