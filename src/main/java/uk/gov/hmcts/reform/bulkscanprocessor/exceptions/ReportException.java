package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class ReportException extends RuntimeException {

    public ReportException(String message, Throwable cause) {
        super(message, cause);
    }
}
