package uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.helper.DirectoryZipper.ZipItem;

import java.io.ByteArrayInputStream;
import java.util.zip.ZipInputStream;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.DirectoryZipper.zipDir;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.DirectoryZipper.zipItems;

@ExtendWith(MockitoExtension.class)
public class ZipExtractorTest {

    private ZipExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new ZipExtractor();
    }

    @Test
    void should_extract_inner_zip_when_wrapping_is_enabled() throws Exception {
        // given
        byte[] innerZip = zipDir("envelopes/sample_valid_content");
        byte[] outerZip = zipItems(singletonList((new ZipItem(ZipExtractor.DOCUMENTS_ZIP, innerZip))));

        // when
        ZipInputStream result = extractor.extract(toZipStream(outerZip));

        // then
        assertThat(result).hasSameContentAs(toZipStream(innerZip));
    }

    private ZipInputStream toZipStream(byte[] bytes) {
        return new ZipInputStream(new ByteArrayInputStream(bytes));
    }
}
