package uk.gov.hmcts.reform.bulkscanprocessor.services.document.output;

import org.springframework.http.MediaType;

import java.util.Arrays;
import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Pdf pdf = (Pdf) o;
        return Objects.equals(fileBaseName, pdf.fileBaseName) && Arrays.equals(bytes, pdf.bytes);
    }

    @Override
    public int hashCode() {

        int result = Objects.hash(fileBaseName);
        result = 31 * result + Arrays.hashCode(bytes);
        return result;
    }
}
