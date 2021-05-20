package uk.gov.hmcts.reform.sendletter.blob.component;

import com.azure.storage.blob.BlobClientBuilder;
import com.azure.storage.blob.models.BlobStorageException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sendletter.model.in.Document;
import uk.gov.hmcts.reform.sendletter.model.in.ManifestBlobInfo;
import uk.gov.hmcts.reform.sendletter.model.in.PrintResponse;
import uk.gov.hmcts.reform.sendletter.services.SasTokenGeneratorService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class BlobBackup {

    private static final Logger LOG = LoggerFactory.getLogger(BlobBackup.class);
    private final SasTokenGeneratorService sasTokenGeneratorService;
    private final BlobManager blobManager;
    private final ObjectMapper mapper;
    private static final String BACKUP_CONTAINER = "backup";

    public BlobBackup(BlobManager blobManager, SasTokenGeneratorService sasTokenGeneratorService, ObjectMapper mapper) {
        this.blobManager =  blobManager;
        this.sasTokenGeneratorService  = sasTokenGeneratorService;
        this.mapper = mapper;
    }

    public PrintResponse backupBlobs(ManifestBlobInfo blobInfo) {
        PrintResponse printResponse = null;
        var destDirectory = "/var/tmp/";
        try {
            var serviceName = blobInfo.getServiceName();
            var containerName = blobInfo.getContainerName();
            var fileName = blobInfo.getBlobName();

            LOG.info("getPdfInfo serviceName {}, containerName {}, blobName {}", serviceName, containerName, fileName);
            var sasToken = sasTokenGeneratorService.generateSasToken(serviceName);
            LOG.info("sasToken code: {}", sasToken);
            var  sourceBlobClient = blobManager.getBlobClient(containerName, sasToken, fileName);

            var blobFile = destDirectory + removeSlashFromBlobName(fileName);
            sourceBlobClient.downloadToFile(blobFile, true);

            var file =  new File(blobFile);
            printResponse = mapper.readValue(file, PrintResponse.class);

            if (printResponse != null && printResponse.printJob != null && printResponse.printJob.documents != null) {
                for (Document m : printResponse.printJob.documents) {
                    var pdfFile = m.uploadToPath;
                    LOG.info("Document FileName {}, NoOfCopies {}, uploadToPath {}", m.fileName, m.copies, pdfFile);
                    doBackup(pdfFile, sasToken, containerName);
                }
                doBackup(fileName, sasToken, containerName);
            }
            cleanUp(file);

        } catch (IOException e) {
            LOG.error("Error occured while performing backup", e);
        }
        return printResponse;
    }

    private void cleanUp(File file) throws IOException {
        var path = Path.of(file.getAbsolutePath());
        Files.delete(path);
    }

    private void doBackup(String pdfFile, String sasToken, String sourceContainerName) {
        LOG.info("About to backup original blob in backup container");
        try {
            var destContainerClient = blobManager.getContainerClient(BACKUP_CONTAINER);
            var sourceBlobClient = new BlobClientBuilder()
                    .endpoint(blobManager.getAccountUrl())
                    .sasToken(sasToken)
                    .containerName(sourceContainerName)
                    .blobName(pdfFile)
                    .buildClient();

            var destBlobClient = destContainerClient.getBlobClient(pdfFile);
            var blob = sourceBlobClient.getBlobUrl();
            destBlobClient.copyFromUrl(blob + "?" + sasToken);
            LOG.info("Blob {} backup completed.", blob);
        } catch (BlobStorageException bse) {
            LOG.error("The specified blob does not exist.", bse);
        }
    }

    //This is only for download locally in /var/tmp.
    private String removeSlashFromBlobName(String filename) {
        if (filename.contains("/")) {
            return filename.substring(filename.lastIndexOf("/") + 1);
        }
        return filename;
    }
}
