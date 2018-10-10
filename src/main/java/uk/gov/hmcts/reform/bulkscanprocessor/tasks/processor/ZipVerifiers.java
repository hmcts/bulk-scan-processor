package uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.google.common.io.ByteStreams.toByteArray;


public class ZipVerifiers {

    public static final String DOCUMENTS_ZIP = "documents.zip";
    public static final String SIGNATURE_SIG = "signature.sig";

    private ZipVerifiers() {
    }

    public static Function<ZipStreamWithSignature, ZipInputStream> getPreprocessor(
        boolean hasSignatureCheck
    ) {
        if (hasSignatureCheck) {
            return ZipVerifiers::signatureCheckVerification;
        }
        return ZipVerifiers::noOpVerification;
    }

    public static ZipInputStream noOpVerification(ZipStreamWithSignature zipWithSignature) {
        return zipWithSignature.zipInputStream;
    }

    public static ZipInputStream signatureCheckVerification(ZipStreamWithSignature zipWithSignature) {
        Map<String, byte[]> zipEntries = extractZipEntries(zipWithSignature.zipInputStream);
        if (!verifyFileNames(zipEntries)) {
            throw new RuntimeException(
                "Zip entries do not match expected file names. "
                + "Actual names = " + zipEntries.keySet()
            );
        }
        if (!verifySignature(zipWithSignature.publicKeyBase64, zipEntries)) {
            throw new RuntimeException("Zip signature failed verification");
        }
        return zipWithSignature.zipInputStream;
    }

    private static Map<String, byte[]> extractZipEntries(ZipInputStream zis) {
        Map<String, byte[]> zipEntries = new HashMap<>();
        ZipEntry zipEntry;
        try {
            while ((zipEntry = zis.getNextEntry()) != null) {
                zipEntries.put(zipEntry.getName(), toByteArray(zis));
            }
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

        return zipEntries;
    }


    static boolean verifyFileNames(Map<String, byte[]> entries) {
        return
            entries.size() == 2
            &&
            entries.keySet().stream()
            .filter(n -> DOCUMENTS_ZIP.equalsIgnoreCase(n) || SIGNATURE_SIG.equalsIgnoreCase(n))
            .count() == 2;
    }

    static boolean verifySignature(String publicKeyBase64, Map<String, byte[]> entries) {
        return verifySignature(
            publicKeyBase64,
            entries.get(DOCUMENTS_ZIP),
            entries.get(SIGNATURE_SIG)
        );
    }

    public static boolean verifySignature(String publicKeyBase64, byte[] data, byte[] signed) {
        PublicKey publicKey = decodePublicKey(publicKeyBase64);
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
            signature.update(data);
            return signature.verify(signed);
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            throw new RuntimeException(e);
        }
    }

    private static PublicKey decodePublicKey(String publicKeyBase64) {
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(publicKeyBase64);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePublic(spec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    public static class ZipStreamWithSignature {
        public final ZipInputStream zipInputStream;
        public final String publicKeyBase64;

        public ZipStreamWithSignature(ZipInputStream zipInputStream, String publicKeyBase64) {
            this.zipInputStream = zipInputStream;
            this.publicKeyBase64 = publicKeyBase64;
        }
    }

}
