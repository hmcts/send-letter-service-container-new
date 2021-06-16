package uk.gov.hmcts.reform.sendletter.blob.component;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.specialized.BlobInputStream;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.function.Function;

import static com.google.common.base.Charsets.UTF_8;
import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BlobBackup2Test {

    private static final String TEST_NEW_CONTAINER = "new-bulkprint";
    private static final String BACKUP_CONTAINER = "backup";
    private static final String TEST_BLOB_NAME = "manifest-/print_job_response.json";
    private static final String TEST_PDF_1 = "33dffc2f-94e0-4584-a973-cc56849ecc0b-sscs-SSC001-mypdf.pdf";
    private static final String TEST_PDF_2 = "33dffc2f-94e0-4584-a973-cc56849ecc0b-sscs-SSC001-1.pdf";

    @Mock
    private BlobManager blobManager;
    @Mock
    private SasTokenGeneratorService sasTokenGeneratorService;
    @Mock
    private BlobInputStream blobInputStream;
    @Mock
    private PrintResponse printResponse;
    @Mock
    private BlobClient blobClient;

    private BlobBackup2 blobBackup;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp()  {
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
        var sasToken = "sasToken";
        given(blobClient.getBlobName()).willReturn(TEST_BLOB_NAME);
        var blobInfo = new BlobInfo(blobClient);
        blobInfo.setLeaseId("LEASE_ID");
        blobInfo.setContainerName(TEST_NEW_CONTAINER);

        given(blobManager.getBlobClient(any(), any(), any())).willReturn(blobClient);
        given(blobClient.openInputStream()).willReturn(blobInputStream);

        mapper = spy(mapper);
        var json = StreamUtils.copyToString(
                new ClassPathResource("print_job_response.json").getInputStream(), UTF_8);

        given(blobInputStream.readAllBytes()).willReturn(json.getBytes());

        lenient().when(mapper.readValue(new ByteArrayInputStream(json.getBytes())
                , PrintResponse.class)).thenReturn(printResponse);

        given(sasTokenGeneratorService.generateSasToken(BACKUP_CONTAINER))
                .willReturn(sasToken);


        var response = blobBackup.backupBlobs(blobInfo);
        assertNotNull(response);

        //verify(blobClient).upload(any(), any());
    }

    private void createAccessTokenConfig() {
        Function<String, TokenConfig> tokenFunction = container -> {
            TokenConfig tokenConfig = new TokenConfig();
            tokenConfig.setValidity(300);
            tokenConfig.setServiceName("send_letter_tests");
            tokenConfig.setNewContainerName(container);
            return tokenConfig;
        };
        AccessTokenProperties accessTokenProperties = new AccessTokenProperties();
        accessTokenProperties.setServiceConfig(
                of(
                        tokenFunction.apply(BACKUP_CONTAINER)
                )
        );
    }
}