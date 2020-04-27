package uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.InvalidZipFilesException;
import uk.gov.hmcts.reform.bulkscanprocessor.helper.DirectoryZipper.ZipItem;

import java.io.ByteArrayInputStream;
import java.util.zip.ZipInputStream;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.DirectoryZipper.zipDir;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.DirectoryZipper.zipDirAndWrap;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.DirectoryZipper.zipItems;

@ExtendWith(MockitoExtension.class)
public class ZipExtractorTest {

    private ZipExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new ZipExtractor(true); // use wrapping zip
    }

    @Test
    void should_extract_inner_zip_when_wrapping_is_enabled() throws Exception {
        // given
        byte[] innerZip = zipDir("signature/sample_valid_content");
        byte[] outerZip = zipItems(singletonList((new ZipItem(ZipExtractor.DOCUMENTS_ZIP, innerZip))));

        // when
        ZipInputStream result = extractor.extract(toZipStream(outerZip));

        // then
        assertThat(result).hasSameContentAs(toZipStream(innerZip));
    }

    @Test
    public void should_throw_exception_if_envelope_is_not_found() throws Exception {
        byte[] innerZip = zipDir("signature/sample_valid_content");
        byte[] outerZip = zipItems(singletonList((new ZipItem("invalid_entry_name", innerZip))));

        assertThatThrownBy(() -> extractor.extract(toZipStream(outerZip)))
            .isInstanceOf(InvalidZipFilesException.class)
            .hasMessageContaining("Zip does not contain envelope");
    }

    @Test
    void should_return_original_zip_if_wrapping_is_disabled() throws Exception {
        // given
        ZipInputStream input = toZipStream(zipDirAndWrap("signature/sample_valid_content"));

        // when
        ZipInputStream result = new ZipExtractor(false).extract(input);

        // then
        assertThat(result).hasSameContentAs(input);
    }

    private ZipInputStream toZipStream(byte[] bytes) {
        return new ZipInputStream(new ByteArrayInputStream(bytes));
    }
}
