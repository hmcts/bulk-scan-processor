package uk.gov.hmcts.reform.bulkscanning.exceptionhandlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import uk.gov.hmcts.reform.bulkscanning.exceptions.ServiceConfigNotFoundException;
import uk.gov.hmcts.reform.bulkscanning.exceptions.UnableToGenerateSasTokenException;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.ResponseEntity.status;

@ControllerAdvice
public class ResponseExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ResponseExceptionHandler.class);

    @ExceptionHandler(UnableToGenerateSasTokenException.class)
    protected ResponseEntity<String> handleUnableToGenerateSasTokenException() {
        return status(INTERNAL_SERVER_ERROR).body("Exception occurred while generating SAS Token");
    }

    @ExceptionHandler(ServiceConfigNotFoundException.class)
    protected ResponseEntity<String> handleServiceConfigNotFoundException(ServiceConfigNotFoundException e) {
        return status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }


    @ExceptionHandler(Exception.class)
    protected ResponseEntity<Void> handleInternalException(Exception exception) {
        log.error(exception.getMessage(), exception);
        return status(INTERNAL_SERVER_ERROR).build();
    }
}
