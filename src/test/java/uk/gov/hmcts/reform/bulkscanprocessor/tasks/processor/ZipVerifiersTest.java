package uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.DocSignatureFailureException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.InvalidZipFilesException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.SignatureException;
import java.util.Base64;
import java.util.Set;
import java.util.zip.ZipInputStream;

import static com.google.common.io.Resources.getResource;
import static com.google.common.io.Resources.toByteArray;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.DirectoryZipper.zipAndSignDir;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.DirectoryZipper.zipDir;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.SigningHelper.signWithSha256Rsa;

@ExtendWith(MockitoExtension.class)
public class ZipVerifiersTest {

    private static final String INVALID_SIGNATURE_MESSAGE = "Zip signature failed verification";
    private static final String INVALID_ZIP_ENTRIES_MESSAGE = "Zip entries do not match expected file names";

    private static String publicKeyBase64;
    private static String xyzPublicKeyBase64;

    @BeforeAll
    public static void setUp() throws IOException {
        publicKeyBase64 =
            Base64.getEncoder().encodeToString(
                toByteArray(getResource("signature/public_key.der"))
            );

        xyzPublicKeyBase64 =
            Base64.getEncoder().encodeToString(
                toByteArray(getResource("signature/xyz_test_public_key.der"))
            );
    }

    @Test
    public void should_verify_signed_file_successfully() throws Exception {
        byte[] test1PdfBytes = toByteArray(getResource("test1.pdf"));
        byte[] test1SigPdfBytes = toByteArray(getResource("signature/test1.pdf.sig"));

        assertThatCode(() ->
            ZipVerifiers.verifySignature(publicKeyBase64, test1PdfBytes, test1SigPdfBytes)
        ).doesNotThrowAnyException();
    }

    @Test
    public void should_not_verify_other_file_successfully() throws Exception {
        byte[] test2PdfBytes = toByteArray(getResource("test2.pdf"));
        byte[] test1SigPdfBytes = toByteArray(getResource("signature/test1.pdf.sig"));
        assertThatThrownBy(() ->
            ZipVerifiers.verifySignature(publicKeyBase64, test2PdfBytes, test1SigPdfBytes)
        )
            .isInstanceOf(DocSignatureFailureException.class)
            .hasMessage("Zip signature failed verification");
    }

    @Test
    public void should_verify_2_valid_filenames_successfully() throws Exception {
        Set<String> files = ImmutableSet.of(
            ZipVerifiers.DOCUMENTS_ZIP,
            ZipVerifiers.SIGNATURE_SIG
        );

        assertThatCode(() -> ZipVerifiers.verifyFileNames(files)).doesNotThrowAnyException();
    }

    @Test
    public void should_not_verify_more_than_2_files_successfully() throws Exception {
        Set<String> files = ImmutableSet.of(
            ZipVerifiers.DOCUMENTS_ZIP,
            ZipVerifiers.SIGNATURE_SIG,
            "signature2"
        );

        assertThatThrownBy(() -> ZipVerifiers.verifyFileNames(files))
            .isInstanceOf(DocSignatureFailureException.class)
            .hasMessageContaining(INVALID_ZIP_ENTRIES_MESSAGE);
    }

    @Test
    public void should_not_verify_invalid_filenames_successfully() throws Exception {
        Set<String> files = ImmutableSet.of(
            ZipVerifiers.DOCUMENTS_ZIP,
            "not" + ZipVerifiers.SIGNATURE_SIG
        );

        assertThatThrownBy(() -> ZipVerifiers.verifyFileNames(files))
            .isInstanceOf(InvalidZipFilesException.class)
            .hasMessageContaining(INVALID_ZIP_ENTRIES_MESSAGE);
    }

    @Test
    public void should_verify_valid_zip_successfully() throws Exception {
        byte[] zipBytes = zipAndSignDir("signature/sample_valid_content", "signature/test_private_key.der");

        ZipVerifiers.ZipStreamWithSignature zipStreamWithSig = new ZipVerifiers.ZipStreamWithSignature(
            new ZipInputStream(new ByteArrayInputStream(zipBytes)), publicKeyBase64, "hello.zip", "some_container"
        );
        ZipInputStream zis = ZipVerifiers.sha256WithRsaVerification(zipStreamWithSig);
        assertThat(zis).isNotNull();
    }

    @Test
    public void should_not_verify_invalid_zip_successfully() throws Exception {
        byte[] zipBytes = zipAndSignDir("signature/sample_valid_content", "signature/some_other_private_key.der");
        ZipVerifiers.ZipStreamWithSignature zipStreamWithSig = new ZipVerifiers.ZipStreamWithSignature(
            new ZipInputStream(new ByteArrayInputStream(zipBytes)), publicKeyBase64, "hello.zip", "x"
        );
        assertThrows(
            DocSignatureFailureException.class,
            () -> ZipVerifiers.sha256WithRsaVerification(zipStreamWithSig)
        );
    }

    @Test
    public void should_verify_valid_test_zip_successfully() throws Exception {
        byte[] zipBytes = zipDir("signature/sample_valid_content");
        byte[] signature = signWithSha256Rsa(
            zipBytes,
            toByteArray(getResource("signature/test_private_key.der"))
        );

        assertThatCode(() ->
            ZipVerifiers.verifySignature(publicKeyBase64, zipBytes, signature)
        ).doesNotThrowAnyException();
    }

    @Test
    public void should_not_verify_invalid_signature_successfully() throws Exception {
        byte[] zipBytes = zipDir("signature/sample_valid_content");
        byte[] otherSignature = toByteArray(getResource("signature/signature"));

        assertThatThrownBy(() ->
            ZipVerifiers.verifySignature(xyzPublicKeyBase64, zipBytes, otherSignature)
        )
            .isInstanceOf(DocSignatureFailureException.class)
            .hasMessage(INVALID_SIGNATURE_MESSAGE);
    }

    @Test
    public void should_not_verify_valid_zip_with_wrong_public_key_successfully() throws Exception {
        byte[] zipBytes = zipDir("signature/sample_valid_content");
        byte[] signature = signWithSha256Rsa(zipBytes, toByteArray(getResource("signature/test_private_key.der")));

        assertThatThrownBy(() ->
            ZipVerifiers.verifySignature(xyzPublicKeyBase64, zipBytes, signature)
        )
            .isInstanceOf(DocSignatureFailureException.class)
            .hasMessage(INVALID_SIGNATURE_MESSAGE);
    }

    @Test
    public void should_handle_sample_prod_signature() throws Exception {
        byte[] prodZip = toByteArray(getResource("signature/prod_test_envelope.zip")); // inner zip
        byte[] prodSignature = toByteArray(getResource("signature/prod_test_signature"));
        String prodPublicKey =
            Base64.getEncoder().encodeToString(toByteArray(getResource("signature/prod_public_key.der")));

        assertThatCode(() ->
            ZipVerifiers.verifySignature(prodPublicKey, prodZip, prodSignature)
        ).doesNotThrowAnyException();
    }

    @Test
    public void should_verify_signature_using_nonprod_public_key_for_file_signed_using_nonprod_private_key()
        throws Exception {
        byte[] nonprodZip = toByteArray(getResource("signature/nonprod_envelope.zip")); // inner zip
        byte[] nonprodSignature = toByteArray(getResource("signature/nonprod_envelope_signature"));
        String nonprodPublicKey =
            Base64.getEncoder().encodeToString(toByteArray(getResource("nonprod_public_key.der")));

        assertThatCode(() ->
            ZipVerifiers.verifySignature(nonprodPublicKey, nonprodZip, nonprodSignature)
        ).doesNotThrowAnyException();
    }

    @Test
    public void should_not_verify_signature_using_wrong_pub_key_for_file_signed_using_nonprod_private_key()
        throws Exception {
        byte[] nonprodZip = toByteArray(getResource("signature/nonprod_envelope.zip")); // inner zip
        byte[] nonprodSignature = toByteArray(getResource("signature/nonprod_envelope_signature"));

        assertThatThrownBy(() ->
            ZipVerifiers.verifySignature(publicKeyBase64, nonprodZip, nonprodSignature)
        )
            .isInstanceOf(DocSignatureFailureException.class)
            .hasMessage(INVALID_SIGNATURE_MESSAGE);
    }

    @Test
    public void should_not_verify_signature_of_the_wrong_length() throws Exception {
        byte[] zipBytes = zipDir("signature/sample_valid_content");
        byte[] tooLongSignature = RandomUtils.nextBytes(256);

        assertThatThrownBy(() ->
            ZipVerifiers.verifySignature(publicKeyBase64, zipBytes, tooLongSignature)
        )
            .isInstanceOf(DocSignatureFailureException.class)
            .hasMessage(INVALID_SIGNATURE_MESSAGE)
            .hasCauseInstanceOf(SignatureException.class);
    }
}
