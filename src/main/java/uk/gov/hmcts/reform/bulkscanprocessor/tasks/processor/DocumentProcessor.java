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
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.output.Pdf;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

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

    public void uploadPdfFiles(List<Pdf> pdfs, List<ScannableItem> scannedItems) {
        Map<String, String> response = documentManagementService.uploadDocuments(pdfs);

        log.info("Document service response with file name and doc url {}", response);

        Set<String> filesWithoutUrl =
            Sets.difference(
                scannedItems.stream().map(it -> it.getFileName()).collect(toSet()),
                response.keySet()
            );

        if (filesWithoutUrl.isEmpty()) {
            scannedItems.forEach(item -> {
                item.setDocumentUrl(response.get(item.getFileName()));
                item.setDocumentUuid(extractDocumentUuid(response.get(item.getFileName())));
            });
            scannableItemRepository.saveAll(scannedItems);
        } else {
            throw new DocumentUrlNotRetrievedException(filesWithoutUrl);
        }
    }

    private String extractDocumentUuid(String documentUrl) {
        //text after the last '/' in the url
        return StringUtils.substringAfterLast(documentUrl, "/");
    }
}
