package uk.gov.hmcts.reform.sendletter.blob.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sendletter.config.AccessTokenProperties;
import uk.gov.hmcts.reform.sendletter.exceptions.BlobProcessException;
import uk.gov.hmcts.reform.sendletter.model.in.BlobInfo;
import uk.gov.hmcts.reform.sendletter.model.in.Document;
import uk.gov.hmcts.reform.sendletter.model.in.PrintResponse;
import uk.gov.hmcts.reform.sendletter.services.SasTokenGeneratorService;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@Component
public class BlobBackup2 {

    private static final Logger LOG = LoggerFactory.getLogger(BlobBackup2.class);
    private final SasTokenGeneratorService sasTokenGeneratorService;
    private final BlobManager blobManager;
    private final ObjectMapper mapper;
    private final String backupContainer;

    public BlobBackup2(BlobManager blobManager,
            SasTokenGeneratorService sasTokenGeneratorService,
            ObjectMapper mapper,
            AccessTokenProperties accessTokenProperties) {
        this.blobManager =  blobManager;
        this.sasTokenGeneratorService  = sasTokenGeneratorService;
        this.mapper = mapper;
        this.backupContainer = accessTokenProperties.getContainerForGivenService("send_letter_backup");
    }

    public PrintResponse backupBlobs(BlobInfo blobInfo) {
        PrintResponse printResponse = null;
        try {
            var sourceBlobClient = blobInfo.getBlobClient();
            var sourceContainerName =  blobInfo.getContainerName();
            var serviceName = blobInfo.getServiceName();
            var manifestBlob = sourceBlobClient.getBlobName();
            LOG.info("back up blobs blobName {}", manifestBlob);

            try (var blobInputStream = sourceBlobClient.openInputStream()) {
                byte[] bytes = blobInputStream.readAllBytes();
                printResponse = mapper.readValue(bytes, PrintResponse.class);

                if (printResponse != null
                        && printResponse.printJob != null
                        && printResponse.printJob.documents != null) {

                    var sourceSasToken = sasTokenGeneratorService.generateSasToken(serviceName);
                    var destSasToken = sasTokenGeneratorService.generateSasToken("send_letter_backup");

                    for (Document m : printResponse.printJob.documents) {
                        var actualPdfFile = m.uploadToPath;

                        LOG.info("Document FileName {}, NoOfCopies {}, pdfContent {}", m.fileName, m.copies,
                                actualPdfFile);
                        doBackup(sourceContainerName, actualPdfFile, sourceSasToken, destSasToken);
                    }
                    //backup manifestBlob
                    var destBlobClient = blobManager.getBlobClient(backupContainer, destSasToken, manifestBlob);
                    destBlobClient.upload(new ByteArrayInputStream(bytes), bytes.length);
                }
            }

        } catch (IOException e) {
            LOG.error("Error occurred while performing backup", e);
        }
        return printResponse;
    }

    private void doBackup(String sourceContainer, String pdfFile, String sourceSasToken, String destSasToken) {
        LOG.info("About to backup original blob in backup container");
        try {
            var sourceBlobClient = blobManager.getBlobClient(sourceContainer, sourceSasToken, pdfFile);
            var destBlobClient = blobManager.getBlobClient(backupContainer, destSasToken, pdfFile);

            try (var blobInputStream = sourceBlobClient.openInputStream()) {
                byte[] bytes = blobInputStream.readAllBytes();
                var byteArrayInputStream = new ByteArrayInputStream(bytes);
                destBlobClient.upload(byteArrayInputStream, bytes.length);
            }

            LOG.info("Blob {} backup completed.", pdfFile);
        } catch (Exception e) {
            throw new BlobProcessException("The specified blob does not exist.", e);
        }
    }
}
