package uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.DirectoryZipper.zipDirAndWrap;

@ExtendWith(MockitoExtension.class)
public class ZipVerifiersTest {
    @Test
    public void should_verify_valid_zip_successfully() throws Exception {
        byte[] zipBytes = zipDirAndWrap("signature/sample_valid_content");

        ZipVerifiers.ZipStreamWithSignature zipStreamWithSig = new ZipVerifiers.ZipStreamWithSignature(
            new ZipInputStream(new ByteArrayInputStream(zipBytes)), "hello.zip", "some_container"
        );
        ZipInputStream zis = ZipVerifiers.extract(zipStreamWithSig);
        assertThat(zis).isNotNull();
    }
}
