package uk.gov.hmcts.reform.bulkscanprocessor.exceptionhandlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import uk.gov.hmcts.reform.authorisation.exceptions.InvalidTokenException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EnvelopeClassificationException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EnvelopeNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EnvelopeStateException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ForbiddenException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.InvalidApiKeyException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.PaymentRecordsException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ServiceConfigNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ServiceJuridictionConfigNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.UnAuthenticatedException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.UnableToGenerateSasTokenException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.errors.ErrorResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.errors.FieldError;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.errors.ModelValidationError;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import static java.util.stream.Collectors.toList;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.ResponseEntity.badRequest;
import static org.springframework.http.ResponseEntity.status;

/**
 * This class handles exceptions thrown by the application.
 */
@ControllerAdvice
public class ResponseExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ResponseExceptionHandler.class);

    /**
     * This exception is thrown when argument annotated with @Valid failed validation.
     * @param exception the method argument not valid exception
     * @param headers the headers
     * @param status the status
     * @param request the request
     * @return the bad request response entity
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
        MethodArgumentNotValidException exception,
        HttpHeaders headers,
        HttpStatus status,
        WebRequest request
    ) {
        List<FieldError> fieldErrors =
            exception
                .getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> new FieldError(err.getField(), err.getDefaultMessage()))
                .collect(toList());

        ModelValidationError error = new ModelValidationError(fieldErrors);

        log.error("Bad request: {}", error);

        return badRequest().body(error);
    }

    /**
     * This exception is thrown when unable to generate SAS token.
     * @return internal server error response entity
     */
    @ExceptionHandler(UnableToGenerateSasTokenException.class)
    protected ResponseEntity<ErrorResponse> handleUnableToGenerateSasTokenException() {
        return status(INTERNAL_SERVER_ERROR).body(new ErrorResponse("Exception occurred while generating SAS Token"));
    }

    /**
     * This exception is thrown when service configuration is not found.
     * @param e the service config not found exception
     * @return bad request response entity
     */
    @ExceptionHandler(ServiceConfigNotFoundException.class)
    protected ResponseEntity<ErrorResponse> handleServiceConfigNotFoundException(ServiceConfigNotFoundException e) {
        return status(BAD_REQUEST).body(new ErrorResponse(e.getMessage()));
    }

    /**
     * This exception is thrown when unauthenticated.
     * @param exc the unauthenticated exception
     * @return unauthorized response entity
     */
    @ExceptionHandler(UnAuthenticatedException.class)
    protected ResponseEntity<Void> handleUnAuthenticatedException(UnAuthenticatedException exc) {
        log.error(exc.getMessage(), exc);
        return status(HttpStatus.UNAUTHORIZED).build();
    }

    /**
     * This exception is thrown when service jurisdiction configuration is not found.
     * @param exc the service jurisdiction config not found exception
     * @return bad request response entity
     */
    @ExceptionHandler(ServiceJuridictionConfigNotFoundException.class)
    protected ResponseEntity<Void> handleServiceJuridictionConfigNotFoundException(
        ServiceJuridictionConfigNotFoundException exc
    ) {
        log.error(exc.getMessage(), exc);
        return status(BAD_REQUEST).build();
    }

    /**
     * This exception is thrown when envelope is not found.
     * @param exc the envelope not found exception
     * @return not found response entity
     */
    @ExceptionHandler(EnvelopeNotFoundException.class)
    protected ResponseEntity<Void> handleEnvelopeNotFound(EnvelopeNotFoundException exc) {
        log.error(exc.getMessage(), exc);
        return status(HttpStatus.NOT_FOUND).build();
    }

    /**
     * This exception is thrown when invalid token.
     * @param exc the invalid token exception
     * @return unauthorized response entity
     */
    @ExceptionHandler(InvalidTokenException.class)
    protected ResponseEntity<ErrorResponse> handleInvalidToken(InvalidTokenException exc) {
        log.error(exc.getMessage(), exc);
        return status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(exc.getMessage()));
    }

    /**
     * This exception is thrown when forbidden.
     * @param exc the forbidden exception
     * @return forbidden response entity
     */
    @ExceptionHandler(ForbiddenException.class)
    protected ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException exc) {
        log.error(exc.getMessage(), exc);
        return status(HttpStatus.FORBIDDEN).body(new ErrorResponse(exc.getMessage()));
    }

    /**
     * This exception is thrown when envelope state is invalid.
     * @param exc the envelope state exception
     * @return conflict response entity
     */
    @ExceptionHandler(EnvelopeStateException.class)
    protected ResponseEntity<ErrorResponse> handleEnvelopeState(EnvelopeStateException exc) {
        log.error(exc.getMessage(), exc);
        return status(HttpStatus.CONFLICT).body(new ErrorResponse(exc.getMessage()));
    }

    /**
     * This exception is thrown when envelope classification is invalid.
     * @param exc the envelope classification exception
     * @return conflict response entity
     */
    @ExceptionHandler(EnvelopeClassificationException.class)
    protected ResponseEntity<ErrorResponse> handleEnvelopeState(EnvelopeClassificationException exc) {
        log.error(exc.getMessage(), exc);
        return status(HttpStatus.CONFLICT).body(new ErrorResponse(exc.getMessage()));
    }

    /**
     * This exception is thrown when internal exception occurs.
     * @param exception the exception
     * @return internal server error response entity
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<Void> handleInternalException(Exception exception) {
        log.error(exception.getMessage(), exception);
        return status(INTERNAL_SERVER_ERROR).build();
    }

    /**
     * This exception is thrown when constraint violation occurs.
     * @param exception the constraint violation exception
     * @return bad request response entity
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    protected ResponseEntity<Map<String, String>> handleInternalException(ConstraintViolationException exception) {
        log.error(exception.getMessage(), exception);
        Map<String, String> errors = new HashMap<>();
        Set<ConstraintViolation<?>> violations = exception.getConstraintViolations();

        for (ConstraintViolation<?> violation : violations) {
            String propertyPath = violation.getPropertyPath().toString();
            String message = violation.getMessage();
            errors.put(propertyPath, message);
        }
        return ResponseEntity.badRequest().body(errors);
    }

    /**
     * This exception is thrown when payment records exception occurs.
     * @param exc the payment records exception
     * @return bad request response entity
     */
    @ExceptionHandler(PaymentRecordsException.class)
    protected ResponseEntity<ErrorResponse> handlePaymentException(PaymentRecordsException exc) {
        log.error(exc.getMessage(), exc);
        return status(BAD_REQUEST).body(new ErrorResponse(exc.getMessage()));
    }

    /**
     * This exception is thrown when invalid API key exception occurs.
     * @param exc the invalid API key exception
     * @return unauthorized response entity
     */
    @ExceptionHandler(InvalidApiKeyException.class)
    protected ResponseEntity<ErrorResponse> handleInvalidApiKeyException(InvalidApiKeyException exc) {
        log.error(exc.getMessage(), exc);
        return status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(exc.getMessage()));
    }
}
