package uk.gov.hmcts.reform.bulkscanprocessor.services.document;

import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Arrays;

public class ByteArrayMultipartFile implements MultipartFile {
    private final byte[] content;
    private final String name;
    private final MediaType contentType;

    ByteArrayMultipartFile(byte[] content, String name, MediaType contentType) {
        this.content = content;
        this.name = name;
        this.contentType = contentType;
    }

    public static ByteArrayMultipartFileBuilder builder() {
        return new ByteArrayMultipartFileBuilder();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getOriginalFilename() {
        return name;
    }

    @Override
    public String getContentType() {
        return contentType.toString();
    }

    @Override
    public boolean isEmpty() {
        return content == null || content.length == 0;
    }

    @Override
    public long getSize() {
        return content.length;
    }

    @Override
    public byte[] getBytes() {
        return content;
    }

    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(content);
    }

    @Override
    public void transferTo(File dest) throws IllegalStateException {
        throw new UnsupportedOperationException("Should only be used for byte array.");
    }

    public byte[] getContent() {
        return this.content;
    }

    public String toString() {
        return "ByteArrayMultipartFile(content="
            + Arrays.toString(this.getContent())
            + ", name=" + this.getName()
            + ", contentType=" + this.getContentType() + ")";
    }

    public static class ByteArrayMultipartFileBuilder {
        private byte[] content;
        private String name;
        private MediaType contentType;

        ByteArrayMultipartFileBuilder() {
        }

        public ByteArrayMultipartFileBuilder content(byte[] content) {
            this.content = content;
            return this;
        }

        public ByteArrayMultipartFileBuilder name(String name) {
            this.name = name;
            return this;
        }

        public ByteArrayMultipartFileBuilder contentType(MediaType contentType) {
            this.contentType = contentType;
            return this;
        }

        public ByteArrayMultipartFile build() {
            return new ByteArrayMultipartFile(content, name, contentType);
        }

        public String toString() {
            return "ByteArrayMultipartFile.ByteArrayMultipartFileBuilder(content=" + Arrays.toString(this.content)
                + ", name=" + this.name
                + ", contentType=" + this.contentType + ")";
        }
    }
}
