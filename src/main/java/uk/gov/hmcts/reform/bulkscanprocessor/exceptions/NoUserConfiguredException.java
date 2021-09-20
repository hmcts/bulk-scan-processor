package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class NoUserConfiguredException extends RuntimeException {
    public NoUserConfiguredException(String jurisdiction) {
        super("No user configured for jurisdiction: " + jurisdiction);
    }
}
