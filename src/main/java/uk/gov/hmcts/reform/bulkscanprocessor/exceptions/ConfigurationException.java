package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

/**
 * An exception to be thrown when there is a configuration error.
 */
public class ConfigurationException extends RuntimeException {

    /**
     * Creates a new instance of the configuration exception.
     * @param message the exception message
     */
    public ConfigurationException(String message) {
        super(message);
    }
}
