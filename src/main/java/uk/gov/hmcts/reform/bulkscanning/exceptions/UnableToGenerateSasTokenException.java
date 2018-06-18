package uk.gov.hmcts.reform.bulkscanning.exceptions;

import uk.gov.hmcts.reform.logging.exception.AlertLevel;
import uk.gov.hmcts.reform.logging.exception.UnknownErrorCodeException;

/**
 * SonarQube reports as error. Max allowed - 5 parents
 */
@SuppressWarnings("squid:MaximumInheritanceDepth")
public class UnableToGenerateSasTokenException extends UnknownErrorCodeException {

    private static final long serialVersionUID = -3484283017479516646L;

    public UnableToGenerateSasTokenException(Throwable e) {
        super(AlertLevel.P1, e);
    }
}
