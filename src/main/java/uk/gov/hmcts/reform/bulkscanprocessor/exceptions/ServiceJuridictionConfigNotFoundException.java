package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

/**
 * An exception to be thrown when service jurisdiction configuration is not found.
 */
public class ServiceJuridictionConfigNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 6403249870901622284L;

    /**
     * Creates a new instance of the exception.
     * @param message the error message
     */
    public ServiceJuridictionConfigNotFoundException(String message) {
        super(message);
    }
}
