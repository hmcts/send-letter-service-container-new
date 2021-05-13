package uk.gov.hmcts.reform.sendletter.exceptions;

public class PdfMergeException extends RuntimeException {
    public PdfMergeException(String message, Throwable cause) {
        super(message, cause);
    }
}
