package uk.gov.hmcts.reform.bulkscanprocessor.services;

public class PDF {

    private final String filename;
    private final byte[] bytes;

    public PDF(String filename, byte[] bytes) {
        this.filename = filename;
        this.bytes = bytes;
    }

    public String getFilename() {
        return filename;
    }

    public byte[] getBytes() {
        return bytes;
    }
}
