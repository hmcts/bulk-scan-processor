package uk.gov.hmcts.reform.bulkscanprocessor.services.document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.DocumentUrlNotRetrievedException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.UnableToUploadDocumentException;
import uk.gov.hmcts.reform.ccd.document.am.model.Classification;

import java.io.File;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

@Service
public class DocumentManagementService {

    private static final Logger log = LoggerFactory.getLogger(DocumentManagementService.class);

    private final DocumentServiceHelper documentServiceHelper;
    private final RestTemplate restTemplate;
    private final String docUploadUrl;

    private static final String CLASSIFICATION = "classification";
    private static final String FILES = "files";
    private static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";

    public DocumentManagementService(
        DocumentServiceHelper documentServiceHelper,
        @Value("${case_document_am.url}") String dmUrl,
        RestTemplate restTemplate
    ) {
        this.documentServiceHelper = documentServiceHelper;
        this.restTemplate = restTemplate;
        this.docUploadUrl = dmUrl + "" + "/cases/documents";
    }

    public Map<String, String> uploadDocuments(
        List<File> pdfs,
        String jurisdiction,
        String container
    ) {

        var credential = documentServiceHelper.createDocumentUploadCredential(
            jurisdiction,
            container
        );
        UploadResponse upload = null;

        try {
            upload = uploadDocs(
                pdfs,
                credential
            );
        } catch (Exception exception) {
            log.error("Exception occurred while uploading documents ", exception);
            throw new UnableToUploadDocumentException(exception.getMessage(), exception);
        }
        List<Document> documents;
        if (upload == null || (documents = upload.getDocuments()) == null) {
            throw new DocumentUrlNotRetrievedException(
                pdfs.stream().map(File::getName).collect(Collectors.toSet())
            );
        }

        log.info(
            "File uploaded to CDAM, is hash null ={}, Url = {}",
            (documents.get(0).hashToken != null),
            documents.get(0).links.self.href
        );

        return createFileUploadResponse(documents);
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

    private UploadResponse uploadDocs(
        List<File> pdfs,
        DocumentUploadCredential credential
    ) {
        Classification classification = Classification.RESTRICTED;
        MultiValueMap<String, Object> body
            = prepareRequest(pdfs, classification, credential.caseTypeId, "BULKSCAN");

        HttpHeaders httpHeaders = setHttpHeaders(credential.idamAccessToken, credential.s2sToken);

        HttpEntity<MultiValueMap<String, Object>> httpEntity = new HttpEntity<>(
            body, httpHeaders
        );

        return this.restTemplate.postForObject(docUploadUrl, httpEntity, UploadResponse.class);
    }

    private HttpHeaders setHttpHeaders(String authorizationToken, String serviceAuth) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, authorizationToken);
        headers.add(SERVICE_AUTHORIZATION, serviceAuth);
        headers.set(HttpHeaders.CONTENT_TYPE, MULTIPART_FORM_DATA_VALUE);
        return headers;
    }

    private static MultiValueMap<String, Object> prepareRequest(
        List<File> pdfs,
        Classification classification,
        String caseType,
        String jurisdiction
    ) {
        MultiValueMap<String, Object> parameters = new LinkedMultiValueMap<>();
        pdfs.stream()
            .map(FileSystemResource::new)
            .forEach(file -> parameters.add(FILES, file));
        parameters.add(CLASSIFICATION, classification.name());
        parameters.add("caseTypeId", caseType);
        parameters.add("jurisdictionId", jurisdiction);
        return parameters;
    }
}
