package uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor;

import com.azure.storage.blob.models.BlobStorageException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.FileSizeExceedMaxUploadLimit;
import uk.gov.hmcts.reform.bulkscanprocessor.helper.DirectoryZipper;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class ZipFileProcessorTest {

    private static final String FOLDER_NAME = "tempwork";
    ZipFileProcessor  zipFileProcessor = new ZipFileProcessor(FOLDER_NAME);

    @Test
    void should_run_provided_function_when_there_is_no_error() throws IOException {
        byte[] zipFile = DirectoryZipper.zipDir("envelopes/sample_valid_content");

        ZipInputStream extractedZis = new ZipInputStream(new ByteArrayInputStream(zipFile));

        var zipFileName = "1_2324_43543.zip";
        var consumer = mock(Consumer.class);
        zipFileProcessor.extractPdfFiles(extractedZis, zipFileName, consumer);
        verify(consumer).accept(any());
        assertThat(new File(FOLDER_NAME + File.separator + zipFileName)).doesNotExist();
    }

    @Test
    void should_throw_exception_when_there_is_error() throws IOException {
        byte[] zipFile = DirectoryZipper.zipDir("envelopes/sample_valid_content");

        ZipInputStream extractedZis = new ZipInputStream(new ByteArrayInputStream(zipFile));

        var zipFileName = "1_2324_43543.zip";
        var consumer = mock(Consumer.class);
        doThrow(new BlobStorageException("Blob error", null, null)).when(consumer).accept(any());

        assertThrows(
            BlobStorageException.class,
            () -> zipFileProcessor.extractPdfFiles(extractedZis, zipFileName, consumer)
        );
        assertThat(new File(FOLDER_NAME + File.separator + zipFileName)).doesNotExist();
    }


    @Test
    void should_throw_file_size_exceed_exception_when_file_is_large() throws IOException {
        File file1 = mock(File.class);
        given(file1.length()).willReturn(200_000_000L);
        File  file2 = mock(File.class);
        given(file2.length()).willReturn(314_572_801L);
        given(file2.getName()).willReturn("mock_file2.pdf");
        List<File> fileList = List.of(file1, file2);
        assertThrows(
            FileSizeExceedMaxUploadLimit.class,
            () -> zipFileProcessor.checkFileSizeAgainstUploadLimit(fileList)
        );
    }
}
