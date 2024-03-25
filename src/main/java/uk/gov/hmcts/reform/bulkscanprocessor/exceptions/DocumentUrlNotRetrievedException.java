package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

import joptsimple.internal.Strings;

import java.util.Set;

/**
 * An exception to be thrown when the document url is not retrieved.
 */
public class DocumentUrlNotRetrievedException extends RuntimeException {

    private static final long serialVersionUID = -6159204377653345638L;

    /**
     * Creates a new instance of the exception.
     * @param missingFiles the missing files
     */
    public DocumentUrlNotRetrievedException(Set<String> missingFiles) {
        super("Error retrieving urls for uploaded files: " + Strings.join(missingFiles, ", "));
    }
}
