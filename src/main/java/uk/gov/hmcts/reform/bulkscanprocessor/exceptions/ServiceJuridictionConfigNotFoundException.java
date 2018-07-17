package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class ServiceJuridictionConfigNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 6403249870901622284L;

    public ServiceJuridictionConfigNotFoundException(String message) {
        super(message);
    }
}
