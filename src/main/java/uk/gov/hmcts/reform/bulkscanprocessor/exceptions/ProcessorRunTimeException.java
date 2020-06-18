package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public abstract class ProcessorRunTimeException extends RuntimeException {

    // might contain sensitive data
    private final String detailMessage;

    public ProcessorRunTimeException(String message) {
        super(message);
        detailMessage = message;

    }

    public ProcessorRunTimeException(String message, Throwable cause) {
        super(message, cause);
        detailMessage = message;
    }


    public ProcessorRunTimeException(String message, String detailMessage) {
        super(message);
        this.detailMessage = detailMessage;
    }

    public String getDetailMessage() {
        return detailMessage;
    }
}
