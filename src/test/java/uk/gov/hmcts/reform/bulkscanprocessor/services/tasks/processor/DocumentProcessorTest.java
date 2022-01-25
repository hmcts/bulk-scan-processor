package uk.gov.hmcts.reform.bulkscanprocessor.services.tasks.processor;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItemRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.DocumentUrlNotRetrievedException;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.DocumentManagementService;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.DocumentProcessor;

import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static com.google.common.io.Resources.getResource;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentProcessorTest {

    @Mock
    private ScannableItemRepository scannableItemRepository;

    @Mock
    private DocumentManagementService documentManagementService;

    private DocumentProcessor documentProcessor;

    @BeforeEach
    void setup() {
        documentProcessor = new DocumentProcessor(
            documentManagementService,
            scannableItemRepository
        );
    }

    @Test
    void should_update_document_uuid_when_doc_response_conntains_matching_file_name_and_doc_url()
        throws Exception {
        //Given
        File test1 = new File(getResource("test1.pdf").toURI());

        List<File> pdfs = ImmutableList.of(test1);

        Map<String, String> response = ImmutableMap.of("test1.pdf", "http://localhost/documents/5fef5f98-e875-4084-b115-47188bc9066b");

        when(documentManagementService.uploadDocuments(pdfs)).thenReturn(response);

        //when
        ScannableItem scannableItem = scannableItem("test1.pdf");
        documentProcessor.uploadPdfFiles(pdfs, ImmutableList.of(scannableItem));

        //then

        //Document uuid should be set by the processor
        scannableItem.setDocumentUuid("5fef5f98-e875-4084-b115-47188bc9066b");

        //Verify scanned item was saved with doc url updated
        verify(scannableItemRepository).saveAll(ImmutableList.of(scannableItem));

        verify(documentManagementService).uploadDocuments(pdfs);
    }

    @Test
    void should_throw_exception_when_doc_response_does_not_contain_matching_file_name_and_doc_url() {
        // given
        List<ScannableItem> scannableItems =
            asList(
                scannableItem("a.pdf"),
                scannableItem("b.pdf"),
                scannableItem("c.pdf")
            );

        // 'c' is missing
        given(documentManagementService.uploadDocuments(any()))
            .willReturn(ImmutableMap.of(
                "a.pdf", "http://localhost/documents/uuida",
                "b.pdf", "http://localhost/documents/uuidb"
            ));

        // when
        Throwable exc = catchThrowable(() -> documentProcessor.uploadPdfFiles(emptyList(), scannableItems));

        // then
        assertThat(exc)
            .isInstanceOf(DocumentUrlNotRetrievedException.class)
            .hasMessageContaining("c.pdf");
    }

    private ScannableItem scannableItem(String fileName) {
        return new ScannableItem(
            "1111002",
            Instant.now(),
            null,
            null,
            null,
            Instant.now(),
            null,
            fileName,
            null,
            null,
            null,
            null
        );
    }
}
