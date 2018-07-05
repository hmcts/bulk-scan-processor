package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class MetadataNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1395897001657755898L;

    public MetadataNotFoundException(String message) {
        super(message);
    }
}
