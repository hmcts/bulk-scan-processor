package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

import org.springframework.web.client.HttpServerErrorException;

public class OcrValidationServerSideException extends RuntimeException {

    public OcrValidationServerSideException(String msg, HttpServerErrorException cause) {
        super(msg, cause);
    }
}
