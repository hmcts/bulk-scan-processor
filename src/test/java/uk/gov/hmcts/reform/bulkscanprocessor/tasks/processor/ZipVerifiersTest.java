package uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.InvalidZipFilesException;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
public class ZipVerifiersTest {

    private static final String INVALID_ZIP_ENTRIES_MESSAGE = "Zip entries do not match expected file names";

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
            .isInstanceOf(InvalidZipFilesException.class)
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
}
