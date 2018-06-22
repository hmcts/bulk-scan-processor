package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class UnableToGenerateSasTokenException extends RuntimeException {

    private static final long serialVersionUID = -3484283017479516646L;

    public UnableToGenerateSasTokenException(Throwable e) {
        super(e);
    }
}
