package uk.gov.hmcts.reform.bulkscanprocessor.services.document.output;

import org.springframework.http.MediaType;

import java.io.File;
import java.util.Objects;

@SuppressWarnings("PMD.ShortClassName")
public class Pdf {
    public static final String CONTENT_TYPE = MediaType.APPLICATION_PDF_VALUE;

    private final String fileBaseName;
    private final File file;

    public Pdf(String fileBaseName, File file) {
        this.fileBaseName = fileBaseName;
        this.file = file;
    }

    public String getFilename() {
        return fileBaseName;
    }

    public File getFile() {
        return file;
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
        return pdf.file.getPath().equals(this.file.getPath());
    }

    @Override
    public int hashCode() {

        int result = Objects.hash(fileBaseName);
        result = 31 * result + this.file.hashCode();
        return result;
    }
}
