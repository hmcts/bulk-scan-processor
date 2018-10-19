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

@RunWith(MockitoJUnitRunner.class)
public class ZipVerifiersTest {

    private String publicKeyBase64 =
        "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDEXfjyDFFigzsmFvTe2cWZ45ggH/XoS/3C6Ur/"
            + "V0egi8k5hnIIgPEOUqhrX5UcQorSX7bIlMped6TtPkYdGs/QI6S5m2uz+6Mjai7ZfACGhYxIs8"
            + "35msqvRsDM0tIle/h3eZJb7iPE0anMWb8MkBYU3D3vAnPdBZxiEIwNMUNzqQIDAQAB";

    private static String xyzPublicKeyBase64;

    @BeforeClass
    public static void setUp() throws IOException {
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
        String zipFileName = "1_24-06-2018-00-00-00.test.zip";
        String container = "signature";
        byte[] test1Bytes = toByteArray(getResource(container + "/" + zipFileName));
        ZipVerifiers.ZipStreamWithSignature zipStreamWithSig = new ZipVerifiers.ZipStreamWithSignature(
            new ZipInputStream(new ByteArrayInputStream(test1Bytes)), publicKeyBase64, zipFileName, container
        );
        ZipInputStream zis = ZipVerifiers.sha256WithRsaVerification(zipStreamWithSig);
        assertThat(zis).isNotNull();
    }

    @Test(expected = DocSignatureFailureException.class)
    public void should_not_verify_invalid_zip_successfully() throws Exception {
        String zipFileName = "2_24-06-2018-00-00-00.test.zip";
        String container = "signature";
        byte[] test1Bytes = toByteArray(getResource(container + "/" + zipFileName));
        ZipVerifiers.ZipStreamWithSignature zipStreamWithSig = new ZipVerifiers.ZipStreamWithSignature(
            new ZipInputStream(new ByteArrayInputStream(test1Bytes)), publicKeyBase64, zipFileName, container
        );
        ZipVerifiers.sha256WithRsaVerification(zipStreamWithSig);
    }

    @Test
    public void should_verify_valid_xyz_test_zip_successfully() throws Exception {
        byte[] test1XyzBytes = toByteArray(getResource("signature/xyz_test_envelope.zip"));
        byte[] test1SigXyzBytes = toByteArray(getResource("signature/xyz_test_signature"));
        assertThat(ZipVerifiers.verifySignature(xyzPublicKeyBase64, test1XyzBytes, test1SigXyzBytes)).isTrue();
    }

    @Test
    public void should_not_verify_invalid_xyz_test_zip_successfully() throws Exception {
        byte[] test1XyzBytes = toByteArray(getResource("signature/envelope.zip"));
        byte[] test1SigXyzBytes = toByteArray(getResource("signature/xyz_test_signature"));
        assertThat(ZipVerifiers.verifySignature(xyzPublicKeyBase64, test1XyzBytes, test1SigXyzBytes)).isFalse();
    }

    @Test
    public void should_not_verify_invalid_xyz_signature_successfully() throws Exception {
        byte[] test1XyzBytes = toByteArray(getResource("signature/xyz_test_envelope.zip"));
        byte[] test1SigXyzBytes = toByteArray(getResource("signature/signature"));
        assertThat(ZipVerifiers.verifySignature(xyzPublicKeyBase64, test1XyzBytes, test1SigXyzBytes)).isFalse();
    }

    @Test
    public void should_not_verify_valid_xyz_test_zip_with_wrong_public_key_successfully() throws Exception {
        byte[] test1XyzBytes = toByteArray(getResource("signature/xyz_test_envelope.zip"));
        byte[] test1SigXyzBytes = toByteArray(getResource("signature/xyz_test_signature"));
        assertThat(ZipVerifiers.verifySignature(publicKeyBase64, test1XyzBytes, test1SigXyzBytes)).isFalse();
    }

}
