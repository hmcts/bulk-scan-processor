package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class DocumentNotFoundException extends RuntimeException {

    private static final long serialVersionUID = -6159204377653345638L;

    public DocumentNotFoundException(String message) {
        super(message);
    }
}
