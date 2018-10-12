package uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.SignatureValidationException;

import java.io.ByteArrayInputStream;
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

    private String exelaPublicKeyBase64 =
        "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDNtN8RHTjPFvYkooMy68Rrs8d9"
        + "PN0nGQgKGjk/JhBGHvxVr4RAw33jgBSNIERnNQWMk/uiBo7gBuSFvDznAgZGDTvo"
        + "0ucMuuN0zbjkyv1s/D/6fmpY54cECNCQRxuht9WP8M5TIC2bQU8bt78fZydWjocQ"
        + "oGHorDu0l3GhSVTePQIDAQAB";

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
        files.put("documents.zip", new byte[0]);
        files.put("signature.sig", new byte[0]);
        assertThat(ZipVerifiers.verifyFileNames(files)).isTrue();
    }

    @Test
    public void should_not_verify_more_than_2_files_successfully() throws Exception {
        Map<String, byte[]> files = new HashMap<>();
        files.put("documents.zip", new byte[0]);
        files.put("signature.sig", new byte[0]);
        files.put("signature2.sig", new byte[0]);
        assertThat(ZipVerifiers.verifyFileNames(files)).isFalse();
    }

    @Test
    public void should_not_verify_invalid_filenames_successfully() throws Exception {
        Map<String, byte[]> files = new HashMap<>();
        files.put("documents.zip", new byte[0]);
        files.put("signature2.sig", new byte[0]);
        assertThat(ZipVerifiers.verifyFileNames(files)).isFalse();
    }

    @Test
    public void should_verify_valid_zip_successfully() throws Exception {
        byte[] test1Bytes = toByteArray(getResource("signature/1_24-06-2018-00-00-00.test.zip"));
        ZipVerifiers.ZipStreamWithSignature zipStreamWithSig = new ZipVerifiers.ZipStreamWithSignature(
            new ZipInputStream(new ByteArrayInputStream(test1Bytes)),
            publicKeyBase64
        );
        ZipInputStream zis = ZipVerifiers.sha256WithRsaVerification(zipStreamWithSig);
        assertThat(zis).isNotNull();
    }

    @Test(expected = SignatureValidationException.class)
    public void should_not_verify_invalid_zip_successfully() throws Exception {
        byte[] test1Bytes = toByteArray(getResource("signature/2_24-06-2018-00-00-00.test.zip"));
        ZipVerifiers.ZipStreamWithSignature zipStreamWithSig = new ZipVerifiers.ZipStreamWithSignature(
            new ZipInputStream(new ByteArrayInputStream(test1Bytes)),
            publicKeyBase64
        );
        ZipVerifiers.sha256WithRsaVerification(zipStreamWithSig);
    }

    @Test
    public void should_verify_valid_exela_test_zip_successfully() throws Exception {
        byte[] test1ExBytes = toByteArray(getResource("signature/exela_test_envelope.zip"));
        byte[] test1SigExBytes = toByteArray(getResource("signature/exela_test_signature"));
        assertThat(ZipVerifiers.verifySignature(exelaPublicKeyBase64, test1ExBytes, test1SigExBytes)).isTrue();
    }

    @Test
    public void should_not_verify_invalid_exela_test_zip_successfully() throws Exception {
        byte[] test1ExBytes = toByteArray(getResource("signature/documents.zip"));
        byte[] test1SigExBytes = toByteArray(getResource("signature/exela_test_signature"));
        assertThat(ZipVerifiers.verifySignature(exelaPublicKeyBase64, test1ExBytes, test1SigExBytes)).isFalse();
    }

    @Test
    public void should_not_verify_invalid_exela_signature_successfully() throws Exception {
        byte[] test1ExBytes = toByteArray(getResource("signature/exela_test_envelope.zip"));
        byte[] test1SigExBytes = toByteArray(getResource("signature/signature.sig"));
        assertThat(ZipVerifiers.verifySignature(exelaPublicKeyBase64, test1ExBytes, test1SigExBytes)).isFalse();
    }

    @Test
    public void should_not_verify_valid_exela_test_zip_with_wrong_public_key_successfully() throws Exception {
        byte[] test1ExBytes = toByteArray(getResource("signature/exela_test_envelope.zip"));
        byte[] test1SigExBytes = toByteArray(getResource("signature/exela_test_signature"));
        assertThat(ZipVerifiers.verifySignature(publicKeyBase64, test1ExBytes, test1SigExBytes)).isFalse();
    }

}
