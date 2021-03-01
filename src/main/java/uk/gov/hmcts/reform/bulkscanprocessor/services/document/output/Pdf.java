package uk.gov.hmcts.reform.bulkscanprocessor.services.document.output;

import java.io.File;

@SuppressWarnings("PMD.ShortClassName")
public class Pdf {

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
}
