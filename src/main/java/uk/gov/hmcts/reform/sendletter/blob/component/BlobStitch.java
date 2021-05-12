package uk.gov.hmcts.reform.sendletter.blob.component;

import com.azure.storage.blob.BlobClientBuilder;
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
public class BlobStitch {

    private static final Logger LOG = LoggerFactory.getLogger(BlobStitch.class);
    private final SasTokenGeneratorService sasTokenGeneratorService;
    private final BlobManager blobManager;
    private final ObjectMapper mapper;
    private static final String BACKUP_CONTAINER = "backup";

    public BlobStitch(BlobManager blobManager, SasTokenGeneratorService sasTokenGeneratorService, ObjectMapper mapper) {
        this.blobManager = blobManager;
        this.sasTokenGeneratorService = sasTokenGeneratorService;
        this.mapper = mapper;
    }

    public void stitchBlobs(PrintResponse printResponse) {
        if (printResponse != null && printResponse.printJob != null && printResponse.printJob.documents != null) {
            for (Document m : printResponse.printJob.documents) {
                var pdfFile = m.uploadToPath;
                LOG.info("Document FileName {}, NoOfCopies {}, uploadToPath {}", m.fileName, m.copies, pdfFile);
            }
        }
    }
}
