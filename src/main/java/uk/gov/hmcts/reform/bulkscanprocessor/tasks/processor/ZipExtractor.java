package uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor;

import org.springframework.stereotype.Component;

import java.util.zip.ZipInputStream;

@Component
public class ZipExtractor {

    public static final String DOCUMENTS_ZIP = "envelope.zip";

    /**
     * Extracts the inner zip.
     */
    public ZipInputStream extract(ZipInputStream zipInputStream) {
        return zipInputStream;
    }

}
