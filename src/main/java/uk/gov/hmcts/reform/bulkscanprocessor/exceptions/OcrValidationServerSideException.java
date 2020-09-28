package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

import org.springframework.web.client.HttpServerErrorException;

/**
 * Thrown in case of OCR validation endpoint response with response status code 5xx.
 */
public class OcrValidationServerSideException extends RuntimeException {

    public OcrValidationServerSideException(String msg, HttpServerErrorException cause) {
        super(msg, cause);
    }
}
