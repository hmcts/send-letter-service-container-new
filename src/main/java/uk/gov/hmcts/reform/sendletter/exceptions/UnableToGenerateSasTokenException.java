package uk.gov.hmcts.reform.sendletter.exceptions;

public class UnableToGenerateSasTokenException extends RuntimeException {

    private static final long serialVersionUID = -3484283017479516646L;

    public UnableToGenerateSasTokenException(Throwable throwable) {
        super(throwable);
    }
}
