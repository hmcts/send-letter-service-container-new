package uk.gov.hmcts.reform.sendletter.blob.component;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.specialized.BlobInputStream;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;
import uk.gov.hmcts.reform.sendletter.config.AccessTokenProperties;
import uk.gov.hmcts.reform.sendletter.config.AccessTokenProperties.TokenConfig;
import uk.gov.hmcts.reform.sendletter.exceptions.BlobProcessException;
import uk.gov.hmcts.reform.sendletter.model.in.BlobInfo;
import uk.gov.hmcts.reform.sendletter.model.in.PrintResponse;
import uk.gov.hmcts.reform.sendletter.services.SasTokenGeneratorService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.function.BiFunction;

import static com.google.common.base.Charsets.UTF_8;
import static java.util.List.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BlobBackupTest {

    private static final String TEST_NEW_CONTAINER = "new-bulkprint";
    private static final String TEST_BACKUP_CONTAINER = "backup";
    private static final String TEST_NEW_SERVICE = "send_letter_tests";
    private static final String TEST_BACKUP_SERVICE = "send_letter_backup";
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
    private BlobInputStream blobInputStream1;
    @Mock
    private BlobInputStream blobInputStream2;
    @Mock
    private PrintResponse printResponse;
    @Mock
    private BlobClient blobClient;
    @Mock
    private BlobClient blobClient1;
    @Mock
    private BlobClient blobClient2;
    @Mock
    private BlobClient destBlobClient;
    @Mock
    private BlobClient destBlobClient1;
    @Mock
    private BlobClient destBlobClient2;

    private AccessTokenProperties accessTokenProperties;
    private BlobBackup blobBackup;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp()  {
        createAccessTokenConfig();
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        blobBackup = new BlobBackup(blobManager,
                sasTokenGeneratorService,
                mapper,
                accessTokenProperties);
    }

    @Test
    void should_copy_blob_with_path_from_new_container_to_backup_container() throws IOException {
        var sasToken = "sasToken";
        given(blobManager.getBlobClient(TEST_NEW_CONTAINER, sasToken, TEST_PDF_1)).willReturn(blobClient1);
        given(blobManager.getBlobClient(TEST_NEW_CONTAINER, sasToken, TEST_PDF_2)).willReturn(blobClient2);

        given(blobClient.getBlobName()).willReturn(TEST_BLOB_NAME);
        var blobInfo = new BlobInfo(blobClient);
        blobInfo.setLeaseId("LEASE_ID");
        blobInfo.setContainerName(TEST_NEW_CONTAINER);
        blobInfo.setServiceName(TEST_NEW_SERVICE);

        given(blobManager.getBlobClient(TEST_BACKUP_CONTAINER, sasToken, TEST_BLOB_NAME)).willReturn(destBlobClient);
        given(blobManager.getBlobClient(TEST_BACKUP_CONTAINER, sasToken, TEST_PDF_1)).willReturn(destBlobClient1);
        given(blobManager.getBlobClient(TEST_BACKUP_CONTAINER, sasToken, TEST_PDF_2)).willReturn(destBlobClient2);

        given(blobClient.openInputStream()).willReturn(blobInputStream);
        given(blobClient1.openInputStream()).willReturn(blobInputStream1);
        given(blobClient2.openInputStream()).willReturn(blobInputStream2);

        mapper = spy(mapper);
        var json = StreamUtils.copyToString(
                new ClassPathResource("print_job_response.json").getInputStream(), UTF_8);

        given(blobInputStream.readAllBytes()).willReturn(json.getBytes());
        given(blobInputStream1.readAllBytes()).willReturn("pdf1".getBytes());
        given(blobInputStream2.readAllBytes()).willReturn("pdf2".getBytes());

        lenient().when(mapper.readValue(new ByteArrayInputStream(json.getBytes()),
                PrintResponse.class)).thenReturn(printResponse);

        given(sasTokenGeneratorService.generateSasToken(TEST_NEW_SERVICE))
                .willReturn(sasToken);
        given(sasTokenGeneratorService.generateSasToken(TEST_BACKUP_SERVICE))
                .willReturn(sasToken);

        var response = blobBackup.backupBlobs(blobInfo);

        assertNotNull(response);
        verify(blobManager, times(5)).getBlobClient(any(), any(), any());
        verify(sasTokenGeneratorService).generateSasToken(TEST_NEW_SERVICE);
        verify(sasTokenGeneratorService).generateSasToken(TEST_BACKUP_SERVICE);

        ArgumentCaptor<ByteArrayInputStream> byteCaptor = ArgumentCaptor.forClass(ByteArrayInputStream.class);
        long dataLength = json.getBytes().length;
        verify(destBlobClient).upload(
                byteCaptor.capture(),
                eq(dataLength));

        ByteArrayInputStream value = byteCaptor.getValue();
        byte[] bytes = value.readAllBytes();
        assertThat(bytes).contains(json.getBytes());

        dataLength = "pdf1".getBytes().length;
        verify(destBlobClient1).upload(
                byteCaptor.capture(),
                eq(dataLength));

        value = byteCaptor.getValue();
        bytes = value.readAllBytes();
        assertThat(bytes).contains("pdf1".getBytes());

        dataLength = "pdf2".getBytes().length;
        verify(destBlobClient2).upload(
                byteCaptor.capture(),
                eq(dataLength));

        value = byteCaptor.getValue();
        bytes = value.readAllBytes();
        assertThat(bytes).contains("pdf2".getBytes());
    }

    @Test
    void should_throw_exception_if_blob_not_found() throws IOException {
        var sasToken = "sasToken";
        given(blobManager.getBlobClient(TEST_NEW_CONTAINER, sasToken, TEST_PDF_1)).willReturn(blobClient1);
        given(blobManager.getBlobClient(TEST_NEW_CONTAINER, sasToken, TEST_PDF_2)).willReturn(blobClient2);

        given(blobClient.getBlobName()).willReturn(TEST_BLOB_NAME);
        var blobInfo = new BlobInfo(blobClient);
        blobInfo.setLeaseId("LEASE_ID");
        blobInfo.setContainerName(TEST_NEW_CONTAINER);
        blobInfo.setServiceName(TEST_NEW_SERVICE);

        given(blobManager.getBlobClient(TEST_BACKUP_CONTAINER, sasToken, TEST_PDF_1)).willReturn(destBlobClient1);
        given(blobManager.getBlobClient(TEST_BACKUP_CONTAINER, sasToken, TEST_PDF_2)).willReturn(destBlobClient2);

        given(blobClient.openInputStream()).willReturn(blobInputStream);
        given(blobClient1.openInputStream()).willReturn(blobInputStream1);
        given(blobClient2.openInputStream()).willReturn(blobInputStream2);

        mapper = spy(mapper);
        var json = StreamUtils.copyToString(
                new ClassPathResource("print_job_response.json").getInputStream(), UTF_8);

        given(blobInputStream.readAllBytes()).willReturn(json.getBytes());
        given(blobInputStream1.readAllBytes()).willReturn("pdf1".getBytes());
        given(blobInputStream2.readAllBytes())
                .willThrow(new RuntimeException("The specified blob does not exist."));

        lenient().when(mapper.readValue(new ByteArrayInputStream(json.getBytes()),
                PrintResponse.class)).thenReturn(printResponse);

        given(sasTokenGeneratorService.generateSasToken(TEST_NEW_SERVICE))
                .willReturn(sasToken);
        given(sasTokenGeneratorService.generateSasToken(TEST_BACKUP_SERVICE))
                .willReturn(sasToken);


        assertThatThrownBy(() -> blobBackup.backupBlobs(blobInfo))
                .isInstanceOf(BlobProcessException.class)
                .hasMessage("The specified blob does not exist.");

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
                        tokenFunction.apply("send_letter_backup", "backup")
                )
        );
    }
}