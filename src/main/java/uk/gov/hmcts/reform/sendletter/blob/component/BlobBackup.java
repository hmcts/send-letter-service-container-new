package uk.gov.hmcts.reform.sendletter.blob.component;

import com.azure.storage.blob.BlobClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sendletter.model.in.Document;
import uk.gov.hmcts.reform.sendletter.model.in.ManifestBlobInfo;
import uk.gov.hmcts.reform.sendletter.model.in.out.PrintResponse;
import uk.gov.hmcts.reform.sendletter.services.SasTokenGeneratorService;

import java.io.File;
import java.io.IOException;

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
        String destDirectory = "./data/";
        try {
            String serviceName = blobInfo.getServiceName();
            String containerName = blobInfo.getContainerName();
            String fileName = blobInfo.getBlobName();


            LOG.info("getPdfInfo serviceName {}, containerName {}, blobName {}", serviceName, containerName, fileName);

            //String destDirectory = Files.createTempDirectory("tmpDirPrefix").toFile().getAbsolutePath();

            var sasToken = sasTokenGeneratorService.generateSasToken(blobInfo.getServiceName());
            LOG.info("sasToken code: {}", sasToken);
            var  sourceBlobClient = blobManager.getBlobClient(containerName, sasToken, fileName);

            var blobFile = destDirectory + fileName;
            sourceBlobClient.downloadToFile(blobFile);

            File file =  new File(blobFile);
            printResponse = mapper.readValue(file, PrintResponse.class);

            if (printResponse != null && printResponse.printJob != null && printResponse.printJob.documents != null) {
                for (Document m : printResponse.printJob.documents) {
                    String pdfFile = m.uploadToPath;
                    LOG.info("Document FileName {}, NoOfCopies {}, uploadToPath {}", m.fileName, m.copies, pdfFile);
                    doBackup(pdfFile, sasToken, containerName);
                }
                doBackup(fileName, sasToken, containerName);
            }
            file.delete();

        } catch (IOException e) {
            LOG.error("Error occured while performing backup", e);
        }
        return printResponse;
    }

    private void doBackup(String pdfFile, String sasToken, String sourceContainerName) {
        LOG.info("About to backup original blob in backup container");

        var destContainerClient = blobManager.getContainerClient(BACKUP_CONTAINER);
        var sourceBlobClient = new BlobClientBuilder()
                .endpoint(blobManager.getAccountUrl())
                .sasToken(sasToken)
                .containerName(sourceContainerName)
                .blobName(pdfFile)
                .buildClient();

        var destBlobClient = destContainerClient.getBlobClient(pdfFile);
        String blob = sourceBlobClient.getBlobUrl();
        destBlobClient.copyFromUrl(blob + "?" + sasToken);
        LOG.info("Blob {} backup completed.", blob);
    }
}
