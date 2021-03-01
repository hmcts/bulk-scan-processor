package uk.gov.hmcts.reform.bulkscanprocessor.services.document;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.UnableToUploadDocumentException;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.output.Pdf;
import uk.gov.hmcts.reform.document.domain.Classification;
import uk.gov.hmcts.reform.document.domain.Document;
import uk.gov.hmcts.reform.document.domain.UploadResponse;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

@Service
public class DocumentManagementService {

    private static final Logger log = LoggerFactory.getLogger(DocumentManagementService.class);

    private final AuthTokenGenerator authTokenGenerator;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String dmUri;

    private static final String CLASSIFICATION = "classification";
    private static final String FILES = "files";
    private static final String DOCUMENTS_PATH = "/documents";
    private static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";
    public static final String USER_ID = "user-id";
    public static final String ROLES = "roles";

    public DocumentManagementService(
        AuthTokenGenerator authTokenGenerator,
        @Value("${document_management.url}") String dmUri,
        RestTemplate restTemplate,
        ObjectMapper objectMapper
    ) {
        this.authTokenGenerator = authTokenGenerator;
        this.dmUri = dmUri;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public Map<String, String> uploadDocuments(List<Pdf> pdfs) {

        String s2sToken = authTokenGenerator.generate();
        try {
            UploadResponse upload = uploadDocs(
                null,
                s2sToken,
                null,
                pdfs
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

    public UploadResponse uploadDocs(
        String authorisation,
        String serviceAuth,
        String userId,
        List<Pdf> pdfs
    ) {
        List<String> roles = Collections.emptyList();
        Classification classification = Classification.RESTRICTED;
        try {
            MultiValueMap<String, Object> parameters = prepareRequest(pdfs, roles, classification);

            HttpHeaders httpHeaders = setHttpHeaders(authorisation, serviceAuth, userId);

            HttpEntity<MultiValueMap<String, Object>> httpEntity = new HttpEntity<>(
                parameters, httpHeaders
            );

            final String t = this.restTemplate.postForObject(dmUri + DOCUMENTS_PATH, httpEntity, String.class);

            return objectMapper.readValue(t, UploadResponse.class);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private HttpHeaders setHttpHeaders(String authorizationToken, String serviceAuth, String userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, authorizationToken);
        headers.add(SERVICE_AUTHORIZATION, serviceAuth);
        headers.add(USER_ID, userId);

        headers.set(HttpHeaders.CONTENT_TYPE, MULTIPART_FORM_DATA_VALUE);

        return headers;
    }

    private static MultiValueMap<String, Object> prepareRequest(
        List<Pdf> pdfs,
        List<String> roles,
        Classification classification
    ) {
        MultiValueMap<String, Object> parameters = new LinkedMultiValueMap<>();
        pdfs.stream()
            .map(pdf -> new FileSystemResource(pdf.getFile()))
            .forEach(file -> parameters.add(FILES, file));
        parameters.add(CLASSIFICATION, classification.name());
        parameters.add(ROLES, roles.stream().collect(Collectors.joining(",")));
        return parameters;
    }
}
