package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class ContainerNotFoundException extends RuntimeException {
    public ContainerNotFoundException() {
        super("No BLOB Container found");
    }
}
