package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

/**
 * An exception to be thrown when service configuration is not found.
 */
public class ServiceConfigNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 2969402004892644814L;

    /**
     * Creates a new instance of the exception.
     * @param message the error message
     */
    public ServiceConfigNotFoundException(String message) {
        super(message);
    }
}
