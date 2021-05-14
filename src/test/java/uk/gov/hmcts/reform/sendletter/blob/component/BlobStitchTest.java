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
import uk.gov.hmcts.reform.sendletter.model.in.PrintResponse;
import uk.gov.hmcts.reform.sendletter.services.SasTokenGeneratorService;
import uk.gov.hmcts.reform.sendletter.services.pdf.PdfCreator;

import java.io.FileWriter;
import java.io.IOException;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.io.Resources.getResource;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

@ExtendWith(MockitoExtension.class)
class BlobStitchTest {

    private static final String NEW_CONTAINER = "new-sscs";
    private static final String PROCESSED_CONTAINER = "processed";
    private static final String BLOB_NAME = "test-mypdf.pdf";
    private static final String TEST_SERVICE_NAME = "sscs";

    @Mock
    private BlobManager blobManager;
    @Mock
    private BlobContainerClient destContainerClient;
    @Mock
    private BlobClient destBlobClient;
    @Mock
    private BlobClient client;
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

        blobStitch = new BlobStitch(blobManager, sasTokenGeneratorService, pdfCreator);

        var sasToken = sasTokenGeneratorService.generateSasToken(TEST_SERVICE_NAME);

        BlobClient sourceBlobClient = new BlobClientBuilder()
                .endpoint(blobManager.getAccountUrl())
                .sasToken(sasToken)
                .containerName(NEW_CONTAINER)
                .blobName(BLOB_NAME)
                .buildClient();
        json = Resources.toString(getResource("print_job_response.json"), UTF_8);
        var objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        response = objectMapper.readValue(json, PrintResponse.class);

        var myWriter = new FileWriter("/var/tmp/33dffc2f-94e0-4584-a973-cc56849ecc0b-sscs-SSC001-mypdf.pdf");
        myWriter.write(json);
        myWriter.close();

        myWriter = new FileWriter("/var/tmp/33dffc2f-94e0-4584-a973-cc56849ecc0b-sscs-SSC001-1.pdf");
        myWriter.write(json);
        myWriter.close();
    }

    @Test
    void should_stitch_blob_together() throws IOException {
        given(pdfCreator.createFromBase64PdfWithCopies(anyList())).willReturn("sendletter".getBytes());
        given(blobManager.getContainerClient(PROCESSED_CONTAINER)).willReturn(destContainerClient);
        given(destContainerClient.getBlobClient(anyString())).willReturn(destBlobClient);
        given(blobManager.getBlobClient(any(), any(), any())).willReturn(client);
        mapper = spy(mapper);
        mapper.registerModule(new JavaTimeModule());
        PrintResponse mockResponse = mock(PrintResponse.class);
        lenient().when(mapper.readValue(json, PrintResponse.class)).thenReturn(mockResponse);

        blobStitch.stitchBlobs(response);
        assertThat(response.printJob.documents.size()).isEqualTo(2);
    }

    @Test
    void should_not_return_stitch_blob_together_when_response_is_null() throws IOException {
        mapper = spy(mapper);
        mapper.registerModule(new JavaTimeModule());
        PrintResponse mockResponse = mock(PrintResponse.class);
        lenient().when(mapper.readValue(json, PrintResponse.class)).thenReturn(mockResponse);
        response = null;
        blobStitch.stitchBlobs(response);
        assertNull(response);
    }

    @Test
    void should_not_return_stitch_blob_together_when_printjob_is_null() throws IOException {
        json = Resources.toString(getResource("print_job_is_null.json"), UTF_8);
        var objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        response = objectMapper.readValue(json, PrintResponse.class);

        mapper = spy(mapper);
        mapper.registerModule(new JavaTimeModule());
        PrintResponse mockResponse = mock(PrintResponse.class);
        lenient().when(mapper.readValue(json, PrintResponse.class)).thenReturn(mockResponse);

        blobStitch.stitchBlobs(response);
        assertNull(response.printJob);
    }

    @Test
    void should_not_return_stitch_blob_together_when_documents_is_null() throws IOException {
        json = Resources.toString(getResource("print_job_response_document_is_null.json"), UTF_8);
        var objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        response = objectMapper.readValue(json, PrintResponse.class);

        mapper = spy(mapper);
        mapper.registerModule(new JavaTimeModule());
        PrintResponse mockResponse = mock(PrintResponse.class);
        lenient().when(mapper.readValue(json, PrintResponse.class)).thenReturn(mockResponse);

        blobStitch.stitchBlobs(response);
        assertNull(response.printJob.documents);
    }

    @Test
    void should_not_return_stitch_blob_together_when_upload_info_is_null() throws IOException {
        json = Resources.toString(getResource("print_job_upload_info_is_null.json"), UTF_8);
        var objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        response = objectMapper.readValue(json, PrintResponse.class);

        mapper = spy(mapper);
        mapper.registerModule(new JavaTimeModule());
        PrintResponse mockResponse = mock(PrintResponse.class);
        lenient().when(mapper.readValue(json, PrintResponse.class)).thenReturn(mockResponse);

        blobStitch.stitchBlobs(response);
        assertNull(response.printUploadInfo);
    }

    @Test
    void should_not_return_stitch_blob_together_when_upload_container_is_null() throws IOException {
        json = Resources.toString(getResource("print_job_upload_to_container_is_null.json"), UTF_8);
        var objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        response = objectMapper.readValue(json, PrintResponse.class);

        mapper = spy(mapper);
        mapper.registerModule(new JavaTimeModule());
        PrintResponse mockResponse = mock(PrintResponse.class);
        lenient().when(mapper.readValue(json, PrintResponse.class)).thenReturn(mockResponse);

        blobStitch.stitchBlobs(response);
        assertNull(response.printUploadInfo.uploadToContainer);
    }


    private void createAccessTokenConfig() {
        AccessTokenProperties.TokenConfig tokenConfig = new AccessTokenProperties.TokenConfig();
        tokenConfig.setValidity(300);
        tokenConfig.setServiceName("sscs");

        accessTokenProperties = new AccessTokenProperties();
        accessTokenProperties.setServiceConfig(singletonList(tokenConfig));
    }
}