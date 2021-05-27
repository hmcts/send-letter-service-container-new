package uk.gov.hmcts.reform.sendletter.exceptions;

public class ServiceConfigNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 2969402004892644814L;

    public ServiceConfigNotFoundException(String message) {
        super(message);
    }
}
