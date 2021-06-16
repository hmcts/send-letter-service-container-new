package uk.gov.hmcts.reform.sendletter.blob.component;

import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.specialized.BlobInputStream;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sendletter.model.in.BlobInfo;
import uk.gov.hmcts.reform.sendletter.model.in.Document;
import uk.gov.hmcts.reform.sendletter.model.in.PrintResponse;
import uk.gov.hmcts.reform.sendletter.services.SasTokenGeneratorService;

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
            @Value("${storage.backup-container}") String backupContainer) {
        this.blobManager =  blobManager;
        this.sasTokenGeneratorService  = sasTokenGeneratorService;
        this.mapper = mapper;
        this.backupContainer = backupContainer;
    }

    public PrintResponse backupBlobs(BlobInfo blobInfo) {
        PrintResponse printResponse = null;
        try {
            var sourceBlobClient = blobInfo.getBlobClient();
            var sourceContainerName =  blobInfo.getContainerName();
            var manifestBlob = sourceBlobClient.getBlobName();
            LOG.info("back up blobs blobName {}", manifestBlob);

            try (BlobInputStream blobInputStream = sourceBlobClient.openInputStream()) {
                printResponse = mapper.readValue(blobInputStream, PrintResponse.class);

                if (printResponse != null && printResponse.printJob != null && printResponse.printJob.documents != null) {
                    var sasToken = sasTokenGeneratorService.generateSasToken(backupContainer);
                    for (Document m : printResponse.printJob.documents) {
                        var actualPdfFile = m.uploadToPath;

                        LOG.info("Document FileName {}, NoOfCopies {}, pdfContent {}", m.fileName, m.copies,
                                actualPdfFile);
                        doBackup(sourceContainerName, actualPdfFile, sasToken);
                    }
                    var destBlobClient = blobManager.getBlobClient(backupContainer, sasToken, manifestBlob);
                    //backup manifestBlob
                    destBlobClient.upload(blobInputStream,blobInputStream.getProperties().getBlobSize());
                }
            }

        } catch (IOException e) {
            LOG.error("Error occurred while performing backup", e);
        }
        return printResponse;
    }

    private void doBackup(String sourceContainer, String pdfFile, String sasToken) {
        LOG.info("About to backup original blob in backup container");
        try {
            var sourceBlobClient = blobManager.getBlobClient(sourceContainer, sasToken, pdfFile);
            var destBlobClient = blobManager.getBlobClient(backupContainer, sasToken, pdfFile);

            try (var blobInputStream = sourceBlobClient.openInputStream()) {
                destBlobClient.upload(blobInputStream, blobInputStream.getProperties().getBlobSize());
            }

            LOG.info("Blob {} backup completed.", pdfFile);
        } catch (BlobStorageException bse) {
            LOG.error("The specified blob does not exist.", bse);
        }
    }
}
