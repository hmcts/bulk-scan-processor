package uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItemRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.DocumentNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.DocumentManagementService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.output.Pdf;

import java.util.List;
import java.util.Map;

@Component
public class DocumentProcessor {

    private final DocumentManagementService documentManagementService;
    private final ScannableItemRepository scannableItemRepository;

    private static final Logger log = LoggerFactory.getLogger(DocumentProcessor.class);

    public DocumentProcessor(
        DocumentManagementService documentManagementService,
        ScannableItemRepository scannableItemRepository
    ) {
        this.documentManagementService = documentManagementService;
        this.scannableItemRepository = scannableItemRepository;
    }

    @Transactional
    public void processPdfFiles(List<Pdf> pdfs, List<ScannableItem> scannedItems) {
        // TODO check scannedItems.size == pdfFiles.size

        Map<String, String> response = documentManagementService.uploadDocuments(pdfs);

        log.info("Document service response with file name and doc url {}", response);

        scannedItems.forEach(item -> {
            if (response.containsKey(item.getFileName())) {
                item.setDocumentUrl(response.get(item.getFileName()));
            } else {
                throw new DocumentNotFoundException("Document metadata not found for file " + item.getFileName());
            }
        });

        scannableItemRepository.saveAll(scannedItems);
    }
}
