package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

/**
 * An exception to be thrown when there is a pending migration script.
 */
public class PendingMigrationScriptException extends RuntimeException {

    /**
     * Creates a new instance of the exception.
     * @param script the script that is pending
     */
    public PendingMigrationScriptException(String script) {
        super("Found migration not yet applied " + script);
    }
}
