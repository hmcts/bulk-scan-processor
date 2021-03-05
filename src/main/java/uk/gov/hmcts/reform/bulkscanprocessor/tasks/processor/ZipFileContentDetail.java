package uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor;

import java.util.List;

public class ZipFileContentDetail {

    private final byte[] metadata;

    public final List<String> pdfFileNames;

    public ZipFileContentDetail(byte[] metadata, List<String> pdfFileNames) {
        this.metadata = metadata;
        this.pdfFileNames = List.copyOf(pdfFileNames);
    }

    public byte[] getMetadata() {
        return metadata;
    }
}
