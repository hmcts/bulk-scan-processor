package uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.DocSignatureFailureException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.SignatureValidationException;

import java.io.ByteArrayInputStream;
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

/**
 * Signed zip archive verification utilities. Currently 2 modes are supported:
 * <ul>
 *  <li>none = no signature verification</li>
 *  <li>sha256withrsa = sha256 + rsa signature verification</li>
 * </ul>
 * With the obvious exclusion of the no verification case, a signed zip
 * archive must include 2 files named envelope.zip and signature. The
 * former is the archive content while the latter is the signature the
 * archive has to be verified against.
 * <p>
 * Some openssl commands related to sha256withrsa signatures:
 * <ul>
 *  <li>Create rsa private key:
 *      openssl genrsa -out private_key.pem 1024
 *  </li>
 *  <li>Generate DER format private key from PEM:
 *      openssl pkcs8 -topk8 -inform PEM -outform DER -in private_key.pem
 *      -out private_key.der -nocrypt
 *  </li>
 *  <li>Generate DER format public key from PEM private key:
 *      openssl rsa -in private_key.pem -pubout -outform DER -out public_key.der
 *  </li>
 *  <li>Generate DER format public key from PEM public key:
 *      openssl rsa -pubin -inform PEM -outform DER -in public_key.pem
 *  </li>
 *  <li>Generate signature for file:
 *      openssl dgst -sha256 -sign private_key.pem -out signature envelope.zip
 *  </li>
 *  <li>Verify file signature:
 *      openssl dgst -sha256 -verify public_key.pem -signature signature envelope.zip
 *  </li>
 * </ul>
 * </p>
 *
 */
public class ZipVerifiers {

    public static final String DOCUMENTS_ZIP = "envelope.zip";
    public static final String SIGNATURE_SIG = "signature";

    private static final Logger log = LoggerFactory.getLogger(ZipVerifiers.class);

    private ZipVerifiers() {
    }

    public static Function<ZipStreamWithSignature, ZipInputStream> getPreprocessor(
        String signatureAlgorithm
    ) {
        if ("sha256withrsa".equalsIgnoreCase(signatureAlgorithm)) {
            return ZipVerifiers::sha256WithRsaVerification;
        } else if ("none".equalsIgnoreCase(signatureAlgorithm)) {
            return ZipVerifiers::noOpVerification;
        }
        throw new SignatureValidationException("Undefined signature verification algorithm");
    }

    static ZipInputStream noOpVerification(ZipStreamWithSignature zipWithSignature) {
        return zipWithSignature.zipInputStream;
    }

    static ZipInputStream sha256WithRsaVerification(ZipStreamWithSignature zipWithSignature) {
        Map<String, byte[]> zipEntries = extractZipEntries(zipWithSignature.zipInputStream);
        if (!verifyFileNames(zipEntries)) {
            log.warn("Signature Failure. Zip entries do not match expected file names. "
                + "Container = {} - File = {} - Actual names = {}",
                zipWithSignature.container, zipWithSignature.zipFileName, zipEntries.keySet()
            );
            throw new DocSignatureFailureException(
                zipWithSignature.container,
                zipWithSignature.zipFileName,
                "Zip entries do not match expected file names. Actual names = " + zipEntries.keySet()
            );
        }
        if (!verifySignature(zipWithSignature.publicKeyBase64, zipEntries)) {
            log.warn("Signature Failure. Zip signature failed verification. "
                + "Container = {} - File = {}",
                zipWithSignature.container, zipWithSignature.zipFileName
            );
            throw new DocSignatureFailureException(
                zipWithSignature.container, zipWithSignature.zipFileName, "Zip signature failed verification"
            );
        }
        return new ZipInputStream(new ByteArrayInputStream(zipEntries.get(DOCUMENTS_ZIP)));
    }

    private static Map<String, byte[]> extractZipEntries(ZipInputStream zis) {
        Map<String, byte[]> zipEntries = new HashMap<>();
        ZipEntry zipEntry;
        try {
            while ((zipEntry = zis.getNextEntry()) != null) {
                zipEntries.put(zipEntry.getName(), toByteArray(zis));
            }
        } catch (IOException ioe) {
            throw new SignatureValidationException(ioe);
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
            throw new SignatureValidationException(e);
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
            throw new SignatureValidationException(e);
        }
    }

    public static class ZipStreamWithSignature {
        public final ZipInputStream zipInputStream;
        public final String publicKeyBase64;
        public final String zipFileName;
        public final String container;

        public ZipStreamWithSignature(
            ZipInputStream zipInputStream,
            String publicKeyBase64,
            String zipFileName,
            String container
        ) {
            this.zipInputStream = zipInputStream;
            this.publicKeyBase64 = publicKeyBase64;
            this.zipFileName = zipFileName;
            this.container = container;
        }
    }

}
