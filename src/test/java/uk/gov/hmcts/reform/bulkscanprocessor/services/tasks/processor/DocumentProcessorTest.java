package uk.gov.hmcts.reform.bulkscanprocessor.services.tasks.processor;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItemRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.DocumentNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.DocumentManagementService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.output.Pdf;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.DocumentProcessor;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import static com.google.common.io.Resources.getResource;
import static com.google.common.io.Resources.toByteArray;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DocumentProcessorTest {

    @Captor
    private ArgumentCaptor<List<Pdf>> pdfCaptor;

    @Mock
    private ScannableItemRepository scannableItemRepository;

    @Mock
    private DocumentManagementService documentManagementService;

    private DocumentProcessor documentProcessor;

    @Before
    public void setup() {
        documentProcessor = new DocumentProcessor(
            documentManagementService,
            scannableItemRepository
        );
    }

    @Test
    public void should_update_document_url_when_doc_response_conntains_matching_file_name_and_doc_url()
        throws Exception {
        //Given
        byte[] test1PdfBytes = toByteArray(getResource("test1.pdf"));

        Map<String, byte[]> pdf = ImmutableMap.of("test1.pdf", test1PdfBytes);

        Map<String, String> response = ImmutableMap.of("test1.pdf", "http://localhost/documents/5fef5f98-e875-4084-b115-47188bc9066b");

        when(documentManagementService.uploadDocuments(anyList())).thenReturn(response);

        //when
        ScannableItem scannableItem = scannableItem();
        documentProcessor.processPdfFiles(pdf, ImmutableList.of(scannableItem));

        //then

        //Document url should be set by the processor
        scannableItem.setDocumentUrl("http://localhost/documents/5fef5f98-e875-4084-b115-47188bc9066b");

        //Verify scanned item was saved with doc url updated
        verify(scannableItemRepository).saveAll(ImmutableList.of(scannableItem));

        verify(documentManagementService).uploadDocuments(pdfCaptor.capture());

        assertThat(pdfCaptor.getValue())
            .hasSize(1)
            .flatExtracting(captured -> ImmutableList.of(captured.getFilename(), captured.getBytes()))
            .containsOnly("test1.pdf", test1PdfBytes);
    }

    @Test
    public void should_throw_doc_not_found_exc_when_doc_response_does_not_contain_matching_file_name_and_doc_url()
        throws Exception {
        //Given
        byte[] test1PdfBytes = toByteArray(getResource("test1.pdf"));

        Map<String, byte[]> pdf = ImmutableMap.of("test1.pdf", test1PdfBytes);

        Map<String, String> response = ImmutableMap.of("test2.pdf", "http://localhost/documents/5fef5f98-e875-4084-b115-47188bc9066b");

        when(documentManagementService.uploadDocuments(anyList())).thenReturn(response);

        //when
        ScannableItem scannableItem = scannableItem();

        //then
        assertThatThrownBy(() -> documentProcessor.processPdfFiles(pdf, ImmutableList.of(scannableItem)))
            .isInstanceOf(DocumentNotFoundException.class)
            .hasMessage("Document metadata not found for file test1.pdf");

        verify(scannableItemRepository, times(0)).saveAll(anyCollection());
    }

    private ScannableItem scannableItem() {
        return new ScannableItem(
            "1111002",
            new Timestamp(System.currentTimeMillis()),
            null,
            null,
            null,
            new Timestamp(System.currentTimeMillis()),
            null,
            "test1.pdf",
            null
        );
    }
}
