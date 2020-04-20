package uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor;

import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.InvalidZipArchiveException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.InvalidZipFilesException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.google.common.io.ByteStreams.toByteArray;
import static java.util.Arrays.asList;

public class ZipVerifiers {

    public static final String DOCUMENTS_ZIP = "envelope.zip";
    public static final String SIGNATURE_SIG = "signature";
    public static final String INVALID_SIGNATURE_MESSAGE = "Zip signature failed verification";

    private ZipVerifiers() {
    }

    /**
     * Checks whether the wrapping zip has expected entries and returns the internal zip.
     */
    public static ZipInputStream verifyAndExtract(ZipStreamWithSignature zipWithSignature) {
        Map<String, byte[]> zipEntries = extractZipEntries(zipWithSignature.zipInputStream);

        verifyFileNames(zipEntries.keySet());

        return new ZipInputStream(new ByteArrayInputStream(zipEntries.get(DOCUMENTS_ZIP)));
    }

    private static Map<String, byte[]> extractZipEntries(ZipInputStream zis) {
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

    static void verifyFileNames(Set<String> fileNames) {
        if (!(fileNames.size() == 2 && fileNames.containsAll(asList(DOCUMENTS_ZIP, SIGNATURE_SIG)))) {
            throw new InvalidZipFilesException(
                "Zip entries do not match expected file names. Actual names = " + fileNames
            );
        }
    }

    // TODO: rename / remove
    public static class ZipStreamWithSignature {
        public final ZipInputStream zipInputStream;
        public final String zipFileName;
        public final String container;

        public ZipStreamWithSignature(
            ZipInputStream zipInputStream,
            String zipFileName,
            String container
        ) {
            this.zipInputStream = zipInputStream;
            this.zipFileName = zipFileName;
            this.container = container;
        }
    }
}
