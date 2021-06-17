package uk.gov.hmcts.reform.sendletter.blob.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sendletter.config.AccessTokenProperties;
import uk.gov.hmcts.reform.sendletter.model.in.DeleteBlob;
import uk.gov.hmcts.reform.sendletter.model.in.Doc;
import uk.gov.hmcts.reform.sendletter.model.in.Document;
import uk.gov.hmcts.reform.sendletter.model.in.PrintResponse;
import uk.gov.hmcts.reform.sendletter.services.SasTokenGeneratorService;
import uk.gov.hmcts.reform.sendletter.services.pdf.PdfCreator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
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
    private final String processedContainer;

    public BlobStitch(BlobManager blobManager,
            SasTokenGeneratorService sasTokenGeneratorService,
            PdfCreator pdfCreator,
            AccessTokenProperties accessTokenProperties) {
        this.blobManager = blobManager;
        this.sasTokenGeneratorService = sasTokenGeneratorService;
        this.pdfCreator = pdfCreator;
        this.processedContainer = accessTokenProperties
                .getContainerForGivenService("send_letter_process");
    }

    public DeleteBlob stitchBlobs(PrintResponse printResponse) throws IOException {
        LOG.info("BlobProcessor:: stitchBlobs");
        List<Doc> docs = new ArrayList<>();
        var deleteBlob = new DeleteBlob();
        if (printResponse != null
                && printResponse.printJob != null
                && printResponse.printJob.documents != null
                && printResponse.printUploadInfo != null
                && printResponse.printUploadInfo.uploadToContainer != null
                && printResponse.printJob.service != null) {

            var containerName = printResponse.printJob.containerName;
            var serviceName = printResponse.printJob.service;
            LOG.info("getPdfInfo serviceName {}, containerName {}", serviceName, containerName);

            var sasToken = sasTokenGeneratorService.generateSasToken(serviceName);

            List<String> blobs = new ArrayList<>();
            for (var document : printResponse.printJob.documents) {
                blobs.add(document.uploadToPath);
                docs.add(getPdfDocuments(containerName, sasToken, document));
            }
            deleteBlob.setServiceName(serviceName);
            deleteBlob.setContainerName(containerName);
            deleteBlob.setBlobName(blobs);

            var finalPdfName = generatePdfName(printResponse.printJob.type, serviceName,
                    printResponse.printJob.id);

            byte[] fromBase64PdfWithCopies = pdfCreator.createFromBase64PdfWithCopies(docs);
            try (var targetStream = new ByteArrayInputStream(fromBase64PdfWithCopies)) {
                var destSasToken = sasTokenGeneratorService.generateSasToken("send_letter_process");
                var blobClient = blobManager.getBlobClient(processedContainer, destSasToken, finalPdfName);
                blobClient.upload(targetStream, fromBase64PdfWithCopies.length);
                LOG.info("Uploaded blob {} to Blob storage completed.", blobClient.getBlobUrl());
            }
        }
        return deleteBlob;
    }

    private Doc getPdfDocuments(String containerName, String sasToken, Document document) throws IOException {
        var fileName = document.uploadToPath;
        var sourceBlobClient = blobManager.getBlobClient(containerName, sasToken, fileName);

        try (var blobInputStream = sourceBlobClient.openInputStream()) {
            byte[] bytes = blobInputStream.readAllBytes();
            return new Doc(bytes, document.copies);
        }
    }

    private String generatePdfName(String type, String serviceName, UUID id) {
        String strippedService = serviceName.replace(SEPARATOR, "");
        return type + SEPARATOR + strippedService + SEPARATOR + id + ".pdf";
    }
}
