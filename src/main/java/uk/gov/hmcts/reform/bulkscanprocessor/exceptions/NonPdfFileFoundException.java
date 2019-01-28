package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class NonPdfFileFoundException extends InvalidEnvelopeException {

    private static final long serialVersionUID = 9143161748679833084L;

    public NonPdfFileFoundException(String zipFileName, String fileName) {
        super("Zip '" + zipFileName + "' contains non-pdf file: " + fileName);
    }
}
