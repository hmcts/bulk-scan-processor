package uk.gov.hmcts.reform.bulkscanprocessor.exceptionhandlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import uk.gov.hmcts.reform.authorisation.exceptions.InvalidTokenException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EnvelopeNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ForbiddenException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.InvalidStatusChangeException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ServiceConfigNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ServiceJuridictionConfigNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.UnAuthenticatedException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.UnableToGenerateSasTokenException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.errors.ErrorResponse;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.ResponseEntity.status;

@ControllerAdvice
public class ResponseExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ResponseExceptionHandler.class);

    @ExceptionHandler(UnableToGenerateSasTokenException.class)
    protected ResponseEntity<ErrorResponse> handleUnableToGenerateSasTokenException() {
        return status(INTERNAL_SERVER_ERROR).body(new ErrorResponse("Exception occurred while generating SAS Token"));
    }

    @ExceptionHandler(ServiceConfigNotFoundException.class)
    protected ResponseEntity<ErrorResponse> handleServiceConfigNotFoundException(ServiceConfigNotFoundException e) {
        return status(BAD_REQUEST).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(UnAuthenticatedException.class)
    protected ResponseEntity<Void> handleUnAuthenticatedException(UnAuthenticatedException exc) {
        log.error(exc.getMessage(), exc);
        return status(HttpStatus.UNAUTHORIZED).build();
    }

    @ExceptionHandler(ServiceJuridictionConfigNotFoundException.class)
    protected ResponseEntity<Void> handleServiceJuridictionConfigNotFoundException(
        ServiceJuridictionConfigNotFoundException exc
    ) {
        log.error(exc.getMessage(), exc);
        return status(BAD_REQUEST).build();
    }

    @ExceptionHandler(EnvelopeNotFoundException.class)
    protected ResponseEntity<Void> handleEnvelopeNotFound(EnvelopeNotFoundException exc) {
        log.error(exc.getMessage(), exc);
        return status(HttpStatus.NOT_FOUND).build();
    }

    @ExceptionHandler(InvalidStatusChangeException.class)
    protected ResponseEntity<ErrorResponse> handleInvalidStatusChange(InvalidStatusChangeException exc) {
        log.error(exc.getMessage(), exc);
        return status(HttpStatus.FORBIDDEN).body(new ErrorResponse(exc.getMessage()));
    }

    @ExceptionHandler(InvalidTokenException.class)
    protected ResponseEntity<ErrorResponse> handleInvalidToken(InvalidTokenException exc) {
        log.error(exc.getMessage(), exc);
        return status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(exc.getMessage()));
    }

    @ExceptionHandler(ForbiddenException.class)
    protected ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException exc) {
        log.error(exc.getMessage(), exc);
        return status(HttpStatus.FORBIDDEN).body(new ErrorResponse(exc.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<Void> handleInternalException(Exception exception) {
        log.error(exception.getMessage(), exception);
        return status(INTERNAL_SERVER_ERROR).build();
    }
}
