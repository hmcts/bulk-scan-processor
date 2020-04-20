package uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor;

import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.InvalidZipArchiveException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.SignatureValidationException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.google.common.io.ByteStreams.toByteArray;

public class ZipVerifiers {

    public static final String DOCUMENTS_ZIP = "envelope.zip";
    public static final String SIGNATURE_SIG = "signature";

    private ZipVerifiers() {
    }

    public static Function<ZipStreamWithSignature, ZipInputStream> getPreprocessor(
        String signatureAlgorithm
    ) {
        if ("sha256withrsa".equalsIgnoreCase(signatureAlgorithm)) {
            return ZipVerifiers::extract;
        } else if ("none".equalsIgnoreCase(signatureAlgorithm)) {
            return ZipVerifiers::noOpVerification;
        }
        throw new SignatureValidationException("Undefined signature verification algorithm");
    }

    static ZipInputStream noOpVerification(ZipStreamWithSignature zipWithSignature) {
        return zipWithSignature.zipInputStream;
    }

    /**
     * Extracts the inner zip.
     */
    static ZipInputStream extract(ZipStreamWithSignature zipWithSignature) {
        Map<String, byte[]> zipEntries = extractZipEntries(zipWithSignature.zipInputStream);

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
