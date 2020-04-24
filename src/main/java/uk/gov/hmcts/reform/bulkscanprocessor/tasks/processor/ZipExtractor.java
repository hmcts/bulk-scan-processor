package uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.InvalidZipArchiveException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.InvalidZipFilesException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.google.common.io.ByteStreams.toByteArray;

@Component
public class ZipExtractor {

    public static final String DOCUMENTS_ZIP = "envelope.zip";

    private final boolean useWrappingZip;

    public ZipExtractor(@Value("${use-wrapping-zip}") boolean useWrappingZip) {
        this.useWrappingZip = useWrappingZip;
    }

    /**
     * Extracts the inner zip.
     */
    public ZipInputStream extract(ZipInputStream zipInputStream) {
        if (!useWrappingZip) {
            return zipInputStream;
        } else {
            Map<String, byte[]> zipEntries = extractZipEntries(zipInputStream);

            if (zipEntries.containsKey(DOCUMENTS_ZIP)) {
                return new ZipInputStream(new ByteArrayInputStream(zipEntries.get(DOCUMENTS_ZIP)));
            } else {
                throw new InvalidZipFilesException(
                    "Zip does not contain envelope. Actual zip entries = " + zipEntries.keySet()
                );
            }
        }
    }

    private Map<String, byte[]> extractZipEntries(ZipInputStream zis) {
        try {
            Map<String, byte[]> zipEntries = new HashMap<>();
            ZipEntry zipEntry;
            while ((zipEntry = zis.getNextEntry()) != null) {
                zipEntries.put(zipEntry.getName(), toByteArray(zis));
            }

            return zipEntries;
        } catch (IOException ioe) {
            throw new InvalidZipArchiveException("Error extracting zip entries", ioe);
        }
    }
}
