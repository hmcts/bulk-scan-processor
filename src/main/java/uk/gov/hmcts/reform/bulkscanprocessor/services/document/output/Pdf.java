package uk.gov.hmcts.reform.bulkscanprocessor.services.document.output;

import org.springframework.http.MediaType;

public class Pdf {
    public static final String CONTENT_TYPE = MediaType.APPLICATION_PDF_VALUE;

    private final String fileBaseName;
    private final byte[] bytes;

    public Pdf(String fileBaseName, byte[] bytes) {
        this.fileBaseName = fileBaseName;
        this.bytes = bytes;
    }

    public String getFilename() {
        return fileBaseName;
    }

    public byte[] getBytes() {
        return bytes;
    }
}
