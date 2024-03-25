package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

/**
 * An exception to be thrown when there is no user configured for a jurisdiction.
 */
public class NoUserConfiguredException extends RuntimeException {
    /**
     * Creates a new instance of the exception.
     * @param jurisdiction the jurisdiction
     */
    public NoUserConfiguredException(String jurisdiction) {
        super("No user configured for jurisdiction: " + jurisdiction);
    }
}
