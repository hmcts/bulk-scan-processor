package uk.gov.hmcts.reform.bulkscanprocessor.services.document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.UnableToUploadDocumentException;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.output.Pdf;
import uk.gov.hmcts.reform.document.DocumentUploadClientApi;
import uk.gov.hmcts.reform.document.domain.Document;
import uk.gov.hmcts.reform.document.domain.UploadResponse;
import uk.gov.hmcts.reform.document.utils.InMemoryMultipartFile;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@Service
public class DocumentManagementService {

    private static final String MULTIPART_FORM_PARAM = "files";

    private final DocumentUploadClientApi documentUploadClientApi;
    private final AuthTokenGenerator authTokenGenerator;

    private static final Logger log = LoggerFactory.getLogger(DocumentManagementService.class);

    public DocumentManagementService(
        AuthTokenGenerator authTokenGenerator,
        DocumentUploadClientApi documentUploadClientApi
    ) {
        this.authTokenGenerator = authTokenGenerator;
        this.documentUploadClientApi = documentUploadClientApi;
    }

    public Map<String, String> uploadDocuments(List<Pdf> pdfs) {
        List<MultipartFile> multipartFiles = pdfs.stream()
            .map(pdf -> new InMemoryMultipartFile(
                MULTIPART_FORM_PARAM,
                pdf.getFilename(),
                Pdf.CONTENT_TYPE,
                pdf.getBytes()
            ))
            .collect(toList());

        try {
            UploadResponse upload = documentUploadClientApi.upload(
                null,
                authTokenGenerator.generate(),
                null,
                multipartFiles
            );

            List<Document> documents = upload.getEmbedded().getDocuments();
            log.debug("File upload response from Document Storage service is {}", documents);

            return createFileUploadResponse(documents);

        } catch (Exception exception) {
            log.error("Exception occurred while uploading documents ", exception);
            throw new UnableToUploadDocumentException(exception.getMessage(), exception);
        }
    }

    private Map.Entry<String, String> createResponse(Document document) {
        return new AbstractMap.SimpleEntry<>(
            document.originalDocumentName,
            document.links.self.href
        );
    }

    private Map<String, String> createFileUploadResponse(List<Document> documents) {
        return documents.stream()
            .map(this::createResponse)
            .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

}
