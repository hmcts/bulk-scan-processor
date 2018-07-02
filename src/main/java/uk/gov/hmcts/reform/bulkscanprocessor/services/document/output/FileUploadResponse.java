package uk.gov.hmcts.reform.bulkscanprocessor.services.document.output;

public class FileUploadResponse {

    private final String fileUrl;

    private final String fileName;

    public FileUploadResponse(
        String fileUrl,
        String fileName
    ) {
        this.fileUrl = fileUrl;
        this.fileName = fileName;
    }
}
