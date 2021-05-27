package uk.gov.hmcts.reform.bulkscanprocessor.services;

import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class FileNamesExtractorTest {

    @Mock
    private BlobContainerClient container;

    @Mock
    private BlobItem blobItem;

    @Mock
    private PagedIterable<BlobItem> pagedIterable;

    @Test
    void should_extract_single_file_name() {
        // given
        given(container.listBlobs()).willReturn(pagedIterable);
        given(pagedIterable.stream()).willReturn(singletonList(blobItem).stream());
        given(blobItem.getName()).willReturn("file.zip");

        // when
        var zipFileNames = FileNamesExtractor.getShuffledZipFileNames(container);

        // then
        assertThat(zipFileNames).containsExactly("file.zip");
    }

    @Test
    void should_handle_empty_file_name() {
        // given
        given(container.listBlobs()).willReturn(pagedIterable);
        given(pagedIterable.stream()).willReturn(singletonList(blobItem).stream());
        given(blobItem.getName()).willReturn("");
        // when
        var zipFileNames = FileNamesExtractor.getShuffledZipFileNames(container);

        // then
        assertThat(zipFileNames).isEmpty();
    }

    @Test
    void should_shuffle_multiple_file_names() {
        // given
        var blob0 = mock(BlobItem.class);
        var blob1 = mock(BlobItem.class);
        var blob2 = mock(BlobItem.class);
        var blob3 = mock(BlobItem.class);
        var blob4 = mock(BlobItem.class);
        var blob5 = mock(BlobItem.class);
        var blob6 = mock(BlobItem.class);
        var blob7 = mock(BlobItem.class);
        var blob8 = mock(BlobItem.class);
        var blob9 = mock(BlobItem.class);
        var blob10 = mock(BlobItem.class);

        var blobs = asList(blob0, blob1, blob2, blob3, blob4, blob5, blob6, blob7, blob8, blob9, blob10);

        given(container.listBlobs()).willReturn(pagedIterable);
        given(pagedIterable.stream()).willReturn(blobs.stream());

        for (var i = 0; i < blobs.size(); i++) {
            given(blobs.get(i).getName()).willReturn("file" + i + ".zip");
        }

        // when
        var zipFileNames = FileNamesExtractor.getShuffledZipFileNames(container);

        // then
        assertThat(zipFileNames).hasSize(blobs.size());

        String[] fileNames = new String[blobs.size()];
        for (var i = 0; i < blobs.size(); i++) {
            fileNames[i] = "file" + i + ".zip";
        }
        // ensure all file names are present in the result
        assertThat(zipFileNames).containsExactlyInAnyOrder(fileNames);

        var isShuffled = false;
        // ensure resulting file names are not in the same order as file names of original blobs
        for (var i = 0; i < blobs.size(); i++) {
            if (!zipFileNames.get(i).equals(fileNames[i])) {
                isShuffled = true;
                break;
            }
        }
        assertThat(isShuffled).isTrue();
    }
}
