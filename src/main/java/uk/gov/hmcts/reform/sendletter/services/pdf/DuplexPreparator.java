package uk.gov.hmcts.reform.sendletter.services.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sendletter.exceptions.DuplexException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Component
public class DuplexPreparator {
    private static final Logger logger = LoggerFactory.getLogger(DuplexPreparator.class);

    /**
     * Adds an extra blank page if the total number of pages is odd.
     */
    public byte[] prepare(byte[] pdf) {
        logger.info("File size is {} KB", pdf.length / 1024);
        try (var pdDoc = PDDocument.load(pdf)) {
            if (pdDoc.getNumberOfPages() % 2 == 1) {
                PDRectangle lastPageMediaBox = pdDoc.getPage(pdDoc.getNumberOfPages() - 1).getMediaBox();
                pdDoc.addPage(new PDPage(lastPageMediaBox));
                var out = new ByteArrayOutputStream();
                pdDoc.save(out);

                return out.toByteArray();

            } else {
                return pdf;
            }
        } catch (IOException exc) {
            throw new DuplexException(exc);
        }
    }
}
