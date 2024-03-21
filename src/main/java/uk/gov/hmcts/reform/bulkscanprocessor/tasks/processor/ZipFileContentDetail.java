package uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor;

import java.util.List;

/**
 * Represents the content of a zip file.
 */
public class ZipFileContentDetail {

    private final byte[] metadata;

    public final List<String> pdfFileNames;

    /**
     * Constructor for the ZipFileContentDetail.
     * @param metadata The metadata
     * @param pdfFileNames The PDF file names
     */
    public ZipFileContentDetail(byte[] metadata, List<String> pdfFileNames) {
        this.metadata = metadata;
        this.pdfFileNames = List.copyOf(pdfFileNames);
    }

    /**
     * Gets the metadata.
     * @return The metadata
     */
    public byte[] getMetadata() {
        return metadata;
    }
}
