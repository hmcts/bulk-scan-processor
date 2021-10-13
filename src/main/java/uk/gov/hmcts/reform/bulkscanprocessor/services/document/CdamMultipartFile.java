package uk.gov.hmcts.reform.bulkscanprocessor.services.document;

import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ZipFileLoadException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class CdamMultipartFile implements MultipartFile {
    private final File file;
    private final String name;
    private final MediaType contentType;

    public CdamMultipartFile(File file, String name, MediaType contentType) {
        this.file = file;
        this.name = name;
        this.contentType = contentType;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getOriginalFilename() {
        return file.getName();
    }

    @Override
    public String getContentType() {
        return contentType.toString();
    }

    @Override
    public boolean isEmpty() {
        throw new UnsupportedOperationException("Not Supported");
    }

    @Override
    public long getSize() {
        throw new UnsupportedOperationException("Not Supported");
    }

    @Override
    public byte[] getBytes() {
        try {
            return Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            throw new ZipFileLoadException("File readAllBytes failed for " + file.getName(), e);
        }
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return  Files.newInputStream(file.toPath());
    }

    @Override
    public void transferTo(File dest) throws IllegalStateException {
        throw new UnsupportedOperationException("Should only be used for byte array.");
    }

    public File getContent() {
        return this.file;
    }


}
