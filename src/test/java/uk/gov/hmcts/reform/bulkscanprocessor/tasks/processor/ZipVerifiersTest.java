package uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.DocSignatureFailureException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipInputStream;

import static com.google.common.io.Resources.getResource;
import static com.google.common.io.Resources.toByteArray;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.DirectoryZipper.zipAndSignDir;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.DirectoryZipper.zipDir;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.SigningHelper.signWithSha256Rsa;

@RunWith(MockitoJUnitRunner.class)
public class ZipVerifiersTest {

    private static String publicKeyBase64;
    private static String xyzPublicKeyBase64;

    @BeforeClass
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
        assertThat(ZipVerifiers.verifySignature(publicKeyBase64, test1PdfBytes, test1SigPdfBytes)).isTrue();
    }

    @Test
    public void should_not_verify_other_file_successfully() throws Exception {
        byte[] test2PdfBytes = toByteArray(getResource("test2.pdf"));
        byte[] test1SigPdfBytes = toByteArray(getResource("signature/test1.pdf.sig"));
        assertThat(ZipVerifiers.verifySignature(publicKeyBase64, test2PdfBytes, test1SigPdfBytes)).isFalse();
    }

    @Test
    public void should_verify_2_valid_filenames_successfully() throws Exception {
        Map<String, byte[]> files = new HashMap<>();
        files.put(ZipVerifiers.DOCUMENTS_ZIP, new byte[0]);
        files.put(ZipVerifiers.SIGNATURE_SIG, new byte[0]);
        assertThat(ZipVerifiers.verifyFileNames(files)).isTrue();
    }

    @Test
    public void should_not_verify_more_than_2_files_successfully() throws Exception {
        Map<String, byte[]> files = new HashMap<>();
        files.put(ZipVerifiers.DOCUMENTS_ZIP, new byte[0]);
        files.put(ZipVerifiers.SIGNATURE_SIG, new byte[0]);
        files.put("signature2", new byte[0]);
        assertThat(ZipVerifiers.verifyFileNames(files)).isFalse();
    }

    @Test
    public void should_not_verify_invalid_filenames_successfully() throws Exception {
        Map<String, byte[]> files = new HashMap<>();
        files.put(ZipVerifiers.DOCUMENTS_ZIP, new byte[0]);
        files.put("signature.sig", new byte[0]);
        assertThat(ZipVerifiers.verifyFileNames(files)).isFalse();
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

    @Test(expected = DocSignatureFailureException.class)
    public void should_not_verify_invalid_zip_successfully() throws Exception {
        byte[] zipBytes = zipAndSignDir("signature/sample_valid_content", "signature/some_other_private_key.der");
        ZipVerifiers.ZipStreamWithSignature zipStreamWithSig = new ZipVerifiers.ZipStreamWithSignature(
            new ZipInputStream(new ByteArrayInputStream(zipBytes)), publicKeyBase64, "hello.zip", "x"
        );
        ZipVerifiers.sha256WithRsaVerification(zipStreamWithSig);
    }

    @Test
    public void should_verify_valid_test_zip_successfully() throws Exception {
        byte[] zipBytes = zipDir("signature/sample_valid_content");
        byte[] signature = signWithSha256Rsa(
            zipBytes,
            toByteArray(getResource("signature/test_private_key.der"))
        );
        assertThat(ZipVerifiers.verifySignature(publicKeyBase64, zipBytes, signature)).isTrue();
    }

    @Test
    public void should_not_verify_invalid_signature_successfully() throws Exception {
        byte[] zipBytes = zipDir("signature/sample_valid_content");
        byte[] otherSignature = toByteArray(getResource("signature/signature"));
        assertThat(ZipVerifiers.verifySignature(xyzPublicKeyBase64, zipBytes, otherSignature)).isFalse();
    }

    @Test
    public void should_not_verify_valid_zip_with_wrong_public_key_successfully() throws Exception {
        byte[] zipBytes = zipDir("signature/sample_valid_content");
        byte[] signature = signWithSha256Rsa(zipBytes, toByteArray(getResource("signature/test_private_key.der")));
        assertThat(ZipVerifiers.verifySignature(xyzPublicKeyBase64, zipBytes, signature)).isFalse();
    }

    @Test
    public void should_handle_sample_prod_signature() throws Exception {
        byte[] prodZip = toByteArray(getResource("signature/prod_test_envelope.zip")); // inner zip
        byte[] prodSignature = toByteArray(getResource("signature/prod_test_signature"));
        String prodPublicKey =
            Base64.getEncoder().encodeToString(toByteArray(getResource("signature/prod_public_key.der")));

        assertThat(ZipVerifiers.verifySignature(prodPublicKey, prodZip, prodSignature)).isTrue();
    }
}
