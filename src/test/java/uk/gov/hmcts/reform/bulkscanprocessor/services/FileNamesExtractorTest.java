package uk.gov.hmcts.reform.bulkscanprocessor.services;

import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.ListBlobItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class FileNamesExtractorTest {

    @Mock
    private CloudBlobContainer container;

    @Mock
    private ListBlobItem blob;

    private FileNamesExtractor fileNamesExtractor;

    @BeforeEach
    void setUp() {
        fileNamesExtractor = new FileNamesExtractor();
    }

    @Test
    void should_extract_single_file_name() {
        // given
        given(container.listBlobs()).willReturn(singletonList(blob));
        given(blob.getUri()).willReturn(URI.create("file.zip"));

        // when
        List<String> zipFileNames = fileNamesExtractor.getZipFileNamesFromContainer(container);

        // then
        assertThat(zipFileNames).containsExactly("file.zip");
    }

    @Test
    void should_handle_empty_file_name() {
        // given
        given(container.listBlobs()).willReturn(singletonList(blob));
        given(blob.getUri()).willReturn(URI.create(""));

        // when
        List<String> zipFileNames = fileNamesExtractor.getZipFileNamesFromContainer(container);

        // then
        assertThat(zipFileNames).isEmpty();
    }

    @Test
    void should_shuffle_multiple_file_names() {
        // given
        ListBlobItem blob0 = mock(ListBlobItem.class);
        ListBlobItem blob1 = mock(ListBlobItem.class);
        ListBlobItem blob2 = mock(ListBlobItem.class);
        ListBlobItem blob3 = mock(ListBlobItem.class);
        ListBlobItem blob4 = mock(ListBlobItem.class);
        List<ListBlobItem> blobs = asList(blob0, blob1, blob2, blob3, blob4);
        given(container.listBlobs()).willReturn(blobs);
        for (int i = 0; i < blobs.size(); i++) {
            given(blobs.get(i).getUri()).willReturn(URI.create("file" + i + ".zip"));
        }

        // when
        List<String> zipFileNames = fileNamesExtractor.getZipFileNamesFromContainer(container);

        // then
        assertThat(zipFileNames).hasSize(blobs.size());
        boolean isShuffled = false;
        // ensure resulting file names are not in the same order as file names of original blobs
        for (int i = 0; i < blobs.size(); i++) {
            if (!zipFileNames.get(i).equals("file" + i + ".zip")) {
                isShuffled = true;
                break;
            }
        }
        assertThat(isShuffled).isTrue();
    }
}
