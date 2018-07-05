package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class NonPdfFileFoundException extends RuntimeException {

    private static final long serialVersionUID = 9143161748679833084L;

    public NonPdfFileFoundException(String message) {
        super(message);
    }
}
