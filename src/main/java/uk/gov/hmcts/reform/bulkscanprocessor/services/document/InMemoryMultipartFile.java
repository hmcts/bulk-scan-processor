package uk.gov.hmcts.reform.bulkscanprocessor.services.document;

import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Objects;

public class InMemoryMultipartFile implements MultipartFile {

    private final String name;
    private final String originalFileName;
    private final String contentType;
    private final byte[] payload;

    public InMemoryMultipartFile(String name, String originalFileName, String contentType, byte[] payload) {
        if (payload == null) {
            throw new IllegalArgumentException("Payload cannot be null.");
        }
        this.name = name;
        this.originalFileName = originalFileName;
        this.contentType = contentType;
        this.payload = payload;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getOriginalFilename() {
        return originalFileName;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public boolean isEmpty() {
        return payload.length == 0;
    }

    @Override
    public long getSize() {
        return payload.length;
    }

    @Override
    public byte[] getBytes() throws IOException {
        return payload;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(payload);
    }

    @Override
    public void transferTo(File dest) throws IOException, IllegalStateException {
        try (FileOutputStream stream = new FileOutputStream(dest)) {
            stream.write(payload);
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final InMemoryMultipartFile that = (InMemoryMultipartFile) o;
        return Objects.equals(name, that.name)
            && Objects.equals(originalFileName, that.originalFileName)
            && Objects.equals(contentType, that.contentType)
            && Arrays.equals(payload, that.payload);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, originalFileName, contentType, payload);
    }
}
