package uk.gov.hmcts.reform.sendletter.blob.component;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.specialized.BlobInputStream;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.io.Resources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;
import uk.gov.hmcts.reform.sendletter.config.AccessTokenProperties;
import uk.gov.hmcts.reform.sendletter.config.AccessTokenProperties.TokenConfig;
import uk.gov.hmcts.reform.sendletter.model.in.BlobInfo;
import uk.gov.hmcts.reform.sendletter.model.in.PrintResponse;
import uk.gov.hmcts.reform.sendletter.services.SasTokenGeneratorService;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.io.Resources.getResource;
import static java.util.List.of;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

@ExtendWith(MockitoExtension.class)
class BlobBackup2Test {

    private static final String TEST_NEW_CONTAINER = "new-bulkprint";
    private static final String BACKUP_CONTAINER = "backup";
    private static final String TEST_BLOB_NAME = "print_job_response.json";
    private static final String TEST_SERVICE_NAME = "sscs";

    private AccessTokenProperties accessTokenProperties;

    @Mock
    private BlobManager blobManager;
    @Mock
    private SasTokenGeneratorService sasTokenGeneratorService;
    @Mock
    private BlobInputStream blobInputStream;
    @Mock
    private BlobContainerClient sourceContainerClient;
    @Mock
    private BlobClient destBlobClient;
    @Mock
    private BlobClient blobClient;

    private BlobBackup2 blobBackup;
    private BlobInfo blobInfo;
    private String sasToken;

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() throws IOException {
        createAccessTokenConfig();
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        blobBackup = new BlobBackup2(blobManager,
                sasTokenGeneratorService,
                mapper,
                BACKUP_CONTAINER);
    }

    @Test
    void should_copy_blob_with_path_from_new_container_to_backup_container() throws IOException {
        String sasToken = "sasToken";
        given(sasTokenGeneratorService.generateSasToken(TEST_NEW_CONTAINER))
                .willReturn(sasToken);
        given(blobManager.getBlobClient(
                TEST_NEW_CONTAINER,
                sasToken,
                TEST_BLOB_NAME
        )).willReturn(blobClient);

        given(blobClient.getBlobName()).willReturn(TEST_BLOB_NAME);
        var blobInfo = new BlobInfo(blobClient);
        blobInfo.setLeaseId("LEASE_ID");
        blobInfo.setContainerName(TEST_NEW_CONTAINER);

        given(blobClient.openInputStream()).willReturn(blobInputStream);


        InputStream inputStream = new ClassPathResource("print_job_response.json").getInputStream();
        PrintResponse printResponse = mapper.readValue(inputStream, PrintResponse.class);

        ObjectMapper objectMapper = mock(ObjectMapper.class);
        given(mapper.readValue(eq(blobInputStream), eq(PrintResponse.class))).willReturn(printResponse);


        var response = blobBackup.backupBlobs(blobInfo);

    }

    private void createAccessTokenConfig() {
        Function<String, TokenConfig> tokenFunction = container -> {
            TokenConfig tokenConfig = new TokenConfig();
            tokenConfig.setValidity(300);
            tokenConfig.setServiceName("send_letter_tests");
            tokenConfig.setNewContainerName(container);
            return tokenConfig;
        };
        accessTokenProperties = new AccessTokenProperties();
        accessTokenProperties.setServiceConfig(
                of(
                        tokenFunction.apply(BACKUP_CONTAINER)
                )
        );
    }
}