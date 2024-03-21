package uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItemRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.DocumentUrlNotRetrievedException;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.DocumentManagementService;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

/**
 * Processes the documents.
 */
@Component
public class DocumentProcessor {

    private final DocumentManagementService documentManagementService;
    private final ScannableItemRepository scannableItemRepository;

    private static final Logger log = LoggerFactory.getLogger(DocumentProcessor.class);

    /**
     * Constructor for the DocumentProcessor.
     * @param documentManagementService The document management service
     * @param scannableItemRepository The scannable item repository
     */
    public DocumentProcessor(
        DocumentManagementService documentManagementService,
        ScannableItemRepository scannableItemRepository
    ) {
        this.documentManagementService = documentManagementService;
        this.scannableItemRepository = scannableItemRepository;
    }

    /**
     * Uploads the pdf files to the document management service.
     * @param pdfs The pdf files
     * @param scannedItems The scanned items
     * @param jurisdiction The jurisdiction
     * @param container The container
     */
    public void uploadPdfFiles(
        List<File> pdfs,
        List<ScannableItem> scannedItems,
        String jurisdiction,
        String container
    ) {
        Map<String, String> response = documentManagementService.uploadDocuments(pdfs, jurisdiction, container);

        log.info("Document service response with file name and doc url {}", response);

        Set<String> filesWithoutUrl =
            Sets.difference(
                scannedItems.stream().map(ScannableItem::getFileName).collect(toSet()),
                response.keySet()
            );

        if (filesWithoutUrl.isEmpty()) {
            scannedItems.forEach(item -> item.setDocumentUuid(extractDocumentUuid(response.get(item.getFileName()))));
            scannableItemRepository.saveAll(scannedItems);
        } else {
            throw new DocumentUrlNotRetrievedException(filesWithoutUrl);
        }
    }

    /**
     * Extracts the document uuid from the document url.
     * @param documentUrl The document url
     * @return The document uuid
     */
    private String extractDocumentUuid(String documentUrl) {
        //text after the last '/' in the url. eg: http://localhost/documents/5fef5f98 returns 5fef5f98
        return StringUtils.substringAfterLast(documentUrl, "/");
    }
}
