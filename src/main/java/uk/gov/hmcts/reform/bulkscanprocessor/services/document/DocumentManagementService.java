package uk.gov.hmcts.reform.bulkscanprocessor.services.document;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.UnableToUploadDocumentException;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.output.Pdf;
import uk.gov.hmcts.reform.document.DocumentUploadClientApi;
import uk.gov.hmcts.reform.document.domain.Classification;
import uk.gov.hmcts.reform.document.domain.Document;
import uk.gov.hmcts.reform.document.domain.UploadResponse;
import uk.gov.hmcts.reform.document.utils.InMemoryMultipartFile;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

@Service
public class DocumentManagementService {

    private static final String MULTIPART_FORM_PARAM = "files";
    private static final Logger log = LoggerFactory.getLogger(DocumentManagementService.class);

    private final DocumentUploadClientApi documentUploadClientApi;
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
        DocumentUploadClientApi documentUploadClientApi,
        @Value("${document_management.url}") String dmUri,
        RestTemplate restTemplate,
        ObjectMapper objectMapper
    ) {
        this.authTokenGenerator = authTokenGenerator;
        this.documentUploadClientApi = documentUploadClientApi;
        this.dmUri = dmUri;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
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

        String s2sToken = authTokenGenerator.generate();
        try {
            UploadResponse upload = uploadDocs(
                null,
                s2sToken,
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

    public UploadResponse uploadDocs(
        String authorisation,
        String serviceAuth,
        String userId,
        List<MultipartFile> files
    ) {
        List<String> roles = Collections.emptyList();
        Classification classification = Classification.RESTRICTED;
        List<InputStreamResource> inputStreamList = new ArrayList<InputStreamResource>();
        try {
            MultiValueMap<String, Object> parameters = prepareRequest(roles, classification);

            for (var file : files) {
                inputStreamList.add(getAsStream(file));
            }

            inputStreamList.stream()
                .forEach(in -> parameters.add(FILES, in));

            HttpHeaders httpHeaders = setHttpHeaders(authorisation, serviceAuth, userId);

            HttpEntity<MultiValueMap<String, Object>> httpEntity = new HttpEntity<>(
                parameters, httpHeaders
            );

            final String t = this.restTemplate.postForObject(dmUri + DOCUMENTS_PATH, httpEntity, String.class);

            return objectMapper.readValue(t, UploadResponse.class);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            inputStreamList.forEach(e -> {
                    try {
                        e.getInputStream().close();
                    } catch (IOException ioException) {
                        log.error("input Stream close error");
                    }
                }
            );
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
        List<String> roles,
        Classification classification
    ) {
        MultiValueMap<String, Object> parameters = new LinkedMultiValueMap<>();
        parameters.add(CLASSIFICATION, classification.name());
        parameters.add(ROLES, roles.stream().collect(Collectors.joining(",")));
        return parameters;
    }

    private static InputStreamResource getAsStream(MultipartFile file) throws IOException {
        return
            new InputStreamResource(file.getInputStream()) {
                @Override
                public long contentLength() {
                    return file.getSize();
                }

                @Override
                public String getFilename() {
                    return file.getName();
                }
            };
    }

}
