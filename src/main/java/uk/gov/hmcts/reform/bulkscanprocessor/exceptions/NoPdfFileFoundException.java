package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class NoPdfFileFoundException extends RuntimeException {

    private static final long serialVersionUID = 9143161748679833084L;

    public NoPdfFileFoundException(String message) {
        super(message);
    }
}
