package uk.gov.hmcts.reform.sendletter.blob.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sendletter.model.in.DeleteBlob;
import uk.gov.hmcts.reform.sendletter.model.in.Doc;
import uk.gov.hmcts.reform.sendletter.model.in.Document;
import uk.gov.hmcts.reform.sendletter.model.in.PrintResponse;
import uk.gov.hmcts.reform.sendletter.services.SasTokenGeneratorService;
import uk.gov.hmcts.reform.sendletter.services.pdf.PdfCreator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class BlobStitch {

    private static final Logger LOG = LoggerFactory.getLogger(BlobStitch.class);
    private final PdfCreator pdfCreator;
    private final SasTokenGeneratorService sasTokenGeneratorService;
    private final BlobManager blobManager;
    private static final String SEPARATOR = "_";
    private static final String DEST_DIRECTORY = "/var/tmp/";
    private static final String PROCESSED_CONTAINER = "processed";

    public BlobStitch(BlobManager blobManager, SasTokenGeneratorService sasTokenGeneratorService,
                      PdfCreator pdfCreator) {
        this.blobManager = blobManager;
        this.sasTokenGeneratorService = sasTokenGeneratorService;
        this.pdfCreator = pdfCreator;
    }

    public DeleteBlob stitchBlobs(PrintResponse printResponse) throws IOException {
        List<Doc> docs = new ArrayList<>();
        var deleteBlob = new DeleteBlob();
        if (printResponse != null && printResponse.printJob != null && printResponse.printJob.documents != null
            && printResponse.printUploadInfo != null && printResponse.printUploadInfo.uploadToContainer != null) {

            var containerName = printResponse.printJob.containerName;
            var serviceName = printResponse.printJob.service;
            LOG.info("getPdfInfo serviceName {}, containerName {}", serviceName, containerName);

            //generate sasToken
            var sasToken = sasTokenGeneratorService.generateSasToken(serviceName);

            //convert pdf to list of byte[]
            List<String> blobs = new ArrayList<>();
            for (var document : printResponse.printJob.documents) {
                blobs.add(document.uploadToPath);
                docs.add(getPdfDocuments(containerName, sasToken, document));
            }
            blobs.add(printResponse.printUploadInfo.manifestPath);
            deleteBlob.setServiceName(serviceName);
            deleteBlob.setContainerName(containerName);
            deleteBlob.setBlobName(blobs);
            //create final pdfname
            var finalPdfName = generatePdfName(printResponse.printJob.type, serviceName,
                    printResponse.printJob.id);

            //pdf file with local file location
            var finalPdfPath = DEST_DIRECTORY + finalPdfName;

            // Create final pdf file from byte[] .e.g stitched
            try (var fileOutputStream = new FileOutputStream(finalPdfPath)) {
                fileOutputStream.write(pdfCreator.createFromBase64PdfWithCopies(docs));
            }

            // Create the container and return a container client object
            var containerClient = blobManager.getContainerClient(PROCESSED_CONTAINER);

            // Get a reference to a blob
            var blobClient = containerClient.getBlobClient(finalPdfName);

            LOG.info("Uploading to Blob storage as blob: {}", blobClient.getBlobUrl());

            // Upload the blob
            blobClient.uploadFromFile(finalPdfPath, true);
            LOG.info("Uploaded blob {} to Blob storage completed.", blobClient.getBlobUrl());

            // delete pdf from local
            cleanUp(finalPdfPath);
        }
        return deleteBlob;
    }

    private Doc getPdfDocuments(String containerName, String sasToken, Document document) throws IOException {
        var fileName = document.uploadToPath;
        var sourceBlobClient = blobManager.getBlobClient(containerName, sasToken, fileName);
        sourceBlobClient.downloadToFile(DEST_DIRECTORY + fileName, true);
        var path = Paths.get(DEST_DIRECTORY + fileName);
        byte[] bytes = Files.readAllBytes(path);
        cleanUp(DEST_DIRECTORY + fileName);
        return new Doc(bytes, document.copies);
    }

    private void cleanUp(String file) throws IOException {
        Files.delete(Path.of(new File(file).getAbsolutePath()));
    }

    private String generatePdfName(String type, String serviceName, UUID id) {
        return type + SEPARATOR + serviceName + SEPARATOR + id + ".pdf";
    }
}
