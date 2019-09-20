package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class InvalidJourneyClassificationException extends RuntimeException {
    public InvalidJourneyClassificationException(String message) {
        super(message);
    }
}
