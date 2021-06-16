package uk.gov.hmcts.reform.sendletter.exceptions;

public class LeaseIdNotPresentException extends RuntimeException {
    public LeaseIdNotPresentException(String message) {
        super(message);
    }
}
