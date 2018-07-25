package uk.gov.hmcts.reform.bulkscanprocessor.services.document;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.IncompleteResponseException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.NoPdfFileFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.UnableToUploadDocumentException;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.output.Pdf;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.StreamSupport.stream;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

@Service
public class DocumentManagementService {

    private static final String MULTIPART_FORM_PARAM = "files";
    private static final String DOCUMENTS_PATH = "/documents";
    private static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";

    private final String dmUri;
    private final RestTemplate restTemplate;
    private final AuthTokenGenerator authTokenGenerator;

    private static final Logger log = LoggerFactory.getLogger(DocumentManagementService.class);

    public DocumentManagementService(
        AuthTokenGenerator authTokenGenerator,
        RestTemplate restTemplate,
        @Value("${dm.api_gateway.url}") String dmUri
    ) {
        this.authTokenGenerator = authTokenGenerator;
        this.restTemplate = restTemplate;
        this.dmUri = dmUri;
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

        HttpEntity<MultiValueMap<String, Object>> httpEntity = new HttpEntity<>(
            prepareRequest(multipartFiles),
            setHttpHeaders()
        );

        try {
            ObjectNode objectNode = restTemplate.postForObject(dmUri + DOCUMENTS_PATH, httpEntity, ObjectNode.class);

            if (objectNode != null) {
                JsonNode documents = objectNode.path("_embedded").path("documents");

                log.info("File upload response from Document Storage service is {}", documents);

                return createFileUploadResponse(documents);
            } else {
                throw new IncompleteResponseException("Did not receive correct ObjectNode response");
            }
        } catch (Exception exception) {
            log.error("Exception occurred while uploading documents ", exception);
            throw new UnableToUploadDocumentException(exception.getMessage(), exception);
        }
    }

    private HttpHeaders setHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(SERVICE_AUTHORIZATION, authTokenGenerator.generate());
        headers.set(CONTENT_TYPE, MULTIPART_FORM_DATA_VALUE);
        return headers;
    }

    private static MultiValueMap<String, Object> prepareRequest(List<MultipartFile> files) {
        MultiValueMap<String, Object> parameters = new LinkedMultiValueMap<>();
        files.stream()
            .map(DocumentManagementService::createMultipartRequestBody)
            .forEach(file -> parameters.add(MULTIPART_FORM_PARAM, file));

        parameters.add("classification", "RESTRICTED");
        return parameters;
    }

    private static HttpEntity<Resource> createMultipartRequestBody(MultipartFile file) {
        String contentType = file.getContentType();

        return new HttpEntity<>(buildByteArrayResource(file), buildPartHeaders(contentType));
    }

    private static HttpHeaders buildPartHeaders(String contentType) {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf(contentType));

        return headers;
    }

    private static ByteArrayResource buildByteArrayResource(MultipartFile file) {
        try {
            return new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };
        } catch (IOException ioException) {
            throw new IllegalStateException(ioException);
        }
    }

    private Map.Entry<String, String> createResponse(JsonNode document) {
        return new AbstractMap.SimpleEntry<>(
            document.get("originalDocumentName").asText(),
            document.get("_links").get("self").get("href").asText()
        );
    }

    private Map<String, String> createFileUploadResponse(JsonNode documents) {
        Stream<JsonNode> filesStream = stream(documents.spliterator(), false);
        return filesStream
            .map(this::createResponse)
            .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }


    public Pdf getDocument(String id, String fileName) {
        if (Strings.isNullOrEmpty(id)) {
            throw new NoPdfFileFoundException("Invalid id [" + id + "]");
        }

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new ByteArrayHttpMessageConverter());
        HttpHeaders headers = new HttpHeaders();
        headers.add(SERVICE_AUTHORIZATION, authTokenGenerator.generate());
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM));
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<byte[]> response =
            restTemplate.exchange(dmUri, HttpMethod.GET, entity, byte[].class, id);

        return new Pdf(fileName, response.getBody());
    }

}
