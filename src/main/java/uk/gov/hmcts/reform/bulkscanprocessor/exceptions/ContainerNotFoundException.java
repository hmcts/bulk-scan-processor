package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class ContainerNotFoundException extends RuntimeException {

    private static final long serialVersionUID = -3411204344653345638L;

    public ContainerNotFoundException() {
        super("No BLOB Container found");
    }
}
