package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class ZipFileLoadException extends RuntimeException {
    public ZipFileLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
