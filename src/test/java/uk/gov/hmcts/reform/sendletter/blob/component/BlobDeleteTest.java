package uk.gov.hmcts.reform.sendletter.blob.component;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobClientBuilder;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

@ExtendWith(MockitoExtension.class)
class BlobDeleteTest {

    private static final String NEW_CONTAINER = "new-sscs";
    private static final String BLOB_NAME = "manifest-/print_job_response.json";
    private static final String TEST_SERVICE_NAME = "sscs";

    private AccessTokenProperties accessTokenProperties;

    @Mock
    private BlobManager blobManager;

    @Mock
    private BlobClient client;

    private BlobClient sourceBlobClient;
    private BlobDelete blobDelete;
    private ManifestBlobInfo blobInfo;
    private String sasToken;

    @BeforeEach
    void setUp() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        given(blobManager.getAccountUrl()).willReturn("http://test.account");


        StorageSharedKeyCredential storageCredentials =
                new StorageSharedKeyCredential("testAccountName", "dGVzdGtleQ==");

        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                .credential(storageCredentials)
                .endpoint("http://test.account")
                .buildClient();

        createAccessTokenConfig();

        var sasTokenGeneratorService = new SasTokenGeneratorService(blobServiceClient,
                accessTokenProperties);
        blobDelete = new BlobDelete(blobManager, sasTokenGeneratorService, mapper);
        sasToken = sasTokenGeneratorService.generateSasToken(TEST_SERVICE_NAME);
        sourceBlobClient = new BlobClientBuilder()
                .endpoint(blobManager.getAccountUrl())
                .sasToken(sasToken)
                .containerName(NEW_CONTAINER)
                .blobName(BLOB_NAME)
                .buildClient();

        blobInfo = new ManifestBlobInfo(TEST_SERVICE_NAME, NEW_CONTAINER, BLOB_NAME);

        mapper = spy(mapper);
        var json = Resources.toString(getResource("print_job_response.json"), UTF_8);
        mapper.registerModule(new JavaTimeModule());
        var myWriter = new FileWriter("/var/tmp/print_job_response.json");
        myWriter.write(json);
        myWriter.close();
        var printResponse = mock(PrintResponse.class);
        lenient().when(mapper.readValue(json, PrintResponse.class)).thenReturn(printResponse);
        given(blobManager.getBlobClient(any(), any(), any())).willReturn(client);
    }

    @Test
    void should_delete_original_blob_with_path_from_new_container() {
        Boolean response = blobDelete.deleteOriginalBlobs(blobInfo);
        assertThat(response.booleanValue()).isSameAs(true);
    }

    @Test
    void should_delete_original_blob_without_path_from_new_container() {
        String blobName = "print_job_response.json";
        sourceBlobClient = new BlobClientBuilder()
                .endpoint(blobManager.getAccountUrl())
                .sasToken(sasToken)
                .containerName(NEW_CONTAINER)
                .blobName(blobName)
                .buildClient();
        blobInfo = new ManifestBlobInfo(TEST_SERVICE_NAME, NEW_CONTAINER, blobName);
        Boolean response = blobDelete.deleteOriginalBlobs(blobInfo);
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