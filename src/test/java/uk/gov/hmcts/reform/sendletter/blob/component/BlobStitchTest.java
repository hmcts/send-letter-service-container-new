package uk.gov.hmcts.reform.sendletter.blob.component;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.specialized.BlobInputStream;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;
import uk.gov.hmcts.reform.sendletter.config.AccessTokenProperties;
import uk.gov.hmcts.reform.sendletter.config.AccessTokenProperties.TokenConfig;
import uk.gov.hmcts.reform.sendletter.model.in.PrintResponse;
import uk.gov.hmcts.reform.sendletter.services.SasTokenGeneratorService;
import uk.gov.hmcts.reform.sendletter.services.pdf.PdfCreator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.function.BiFunction;

import static com.google.common.base.Charsets.UTF_8;
import static java.util.List.of;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

@ExtendWith(MockitoExtension.class)
class BlobStitchTest {

    private static final String TEST_NEW_CONTAINER = "new-sscs";
    private static final String TEST_PROCESSED_CONTAINER = "processed";
    private static final String TEST_STITCHED_BLOB_NAME = "SSC001_sscs_33dffc2f-94e0-4584-a973-cc56849ecc0b.pdf";
    private static final String TEST_SERVICE_NAME = "send_letter_process";
    private static final String TEST_PDF_1 = "33dffc2f-94e0-4584-a973-cc56849ecc0b-sscs-SSC001-mypdf.pdf";
    private static final String TEST_PDF_2 = "33dffc2f-94e0-4584-a973-cc56849ecc0b-sscs-SSC001-1.pdf";

    @Mock
    private SasTokenGeneratorService sasTokenGeneratorService;
    @Mock
    private BlobManager blobManager;
    @Mock
    private BlobClient blobClient1;
    @Mock
    private BlobClient blobClient2;
    @Mock
    private BlobClient destBlobClient;
    @Mock
    private BlobInputStream blobInputStream1;
    @Mock
    private BlobInputStream blobInputStream2;
    @Mock
    private PdfCreator pdfCreator;

    private AccessTokenProperties accessTokenProperties;
    private BlobStitch blobStitch;
    private PrintResponse response;
    private ObjectMapper mapper;
    private String json;

    @BeforeEach
    void setUp() throws IOException {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        createAccessTokenConfig();

        blobStitch = new BlobStitch(blobManager,
                sasTokenGeneratorService,
                pdfCreator,
                accessTokenProperties);

        mapper = spy(mapper);
        json = StreamUtils.copyToString(
                new ClassPathResource("print_job_response.json").getInputStream(), UTF_8);
        response = mapper.readValue(new ByteArrayInputStream(json.getBytes()),
                PrintResponse.class);
    }

    @Test
    void should_stitch_blob_together() throws IOException {
        var sasToken = "sasToken";
        given(sasTokenGeneratorService.generateSasToken("sscs")).willReturn(sasToken);
        given(blobManager.getBlobClient(TEST_NEW_CONTAINER, sasToken, TEST_PDF_1)).willReturn(blobClient1);
        given(blobManager.getBlobClient(TEST_NEW_CONTAINER, sasToken, TEST_PDF_2)).willReturn(blobClient2);
        given(blobClient1.openInputStream()).willReturn(blobInputStream1);
        given(blobClient2.openInputStream()).willReturn(blobInputStream2);
        given(blobInputStream1.readAllBytes()).willReturn("pdf1".getBytes());
        given(blobInputStream2.readAllBytes()).willReturn("pdf2".getBytes());

        given(pdfCreator.createFromBase64PdfWithCopies(anyList())).willReturn("sendletter".getBytes());
        given(sasTokenGeneratorService.generateSasToken(TEST_SERVICE_NAME)).willReturn(sasToken);
        given(blobManager.getBlobClient(TEST_PROCESSED_CONTAINER, sasToken,
                TEST_STITCHED_BLOB_NAME)).willReturn(destBlobClient);

        mapper = spy(mapper);
        mapper.registerModule(new JavaTimeModule());
        PrintResponse mockResponse = mock(PrintResponse.class);
        lenient().when(mapper.readValue(json, PrintResponse.class)).thenReturn(mockResponse);

        var deleteBlob = blobStitch.stitchBlobs(response);
        assertThat(deleteBlob.getBlobName().size()).isEqualTo(2);
    }

    @Test
    void should_not_return_stitch_blob_together_when_response_is_null() throws IOException {
        var deleteBlob = blobStitch.stitchBlobs(null);
        assertNull(deleteBlob.getBlobName());
    }

    @ParameterizedTest
    @MethodSource("stringArrayProvider")
    void should_not_return_stitch_blob_together(String testJson) throws IOException {
        json = StreamUtils.copyToString(
                new ClassPathResource(testJson).getInputStream(), UTF_8);
        response = mapper.readValue(json, PrintResponse.class);

        var deleteBlob = blobStitch.stitchBlobs(response);
        assertNull(deleteBlob.getBlobName());
    }

    static String[] stringArrayProvider() {
        return new String[] {"print_job_response_document_is_null.json",
            "print_job_upload_info_is_null.json",
            "print_job_upload_to_container_is_null.json",
            "print_job_is_null.json"};
    }

    private void createAccessTokenConfig() {
        BiFunction<String, String, TokenConfig> tokenFunction = (service, container) -> {
            TokenConfig tokenConfig = new TokenConfig();
            tokenConfig.setValidity(300);
            tokenConfig.setServiceName(service);
            tokenConfig.setContainerName(container);
            return tokenConfig;
        };
        accessTokenProperties = new AccessTokenProperties();
        accessTokenProperties.setServiceConfig(
                of(
                        tokenFunction.apply("send_letter_process", "processed")
                )
        );
    }
}