package uk.gov.hmcts.reform.bulkscanning.exceptions;

import uk.gov.hmcts.reform.logging.exception.AlertLevel;
import uk.gov.hmcts.reform.logging.exception.UnknownErrorCodeException;

/**
 * SonarQube reports as error. Max allowed - 5 parents
 */
@SuppressWarnings("squid:MaximumInheritanceDepth")
public class ServiceConfigNotFoundException extends UnknownErrorCodeException {

    private static final long serialVersionUID = 2969402004892644814L;

    public ServiceConfigNotFoundException(String message) {
        super(AlertLevel.P1, message);
    }
}
