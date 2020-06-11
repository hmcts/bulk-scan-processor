package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class MetadataNotFoundException extends ZipFileProcessingFailedException {
    private static final long serialVersionUID = 1395897001657755898L;

    public MetadataNotFoundException(String message) {
        super(message);
    }
}
