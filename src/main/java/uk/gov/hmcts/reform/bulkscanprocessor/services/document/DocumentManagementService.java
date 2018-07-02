package uk.gov.hmcts.reform.bulkscanprocessor.services.document;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.hateoas.hal.HalLinkDiscoverer;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.UnableToUploadDocumentException;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.output.FileUploadResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.output.Pdf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.StreamSupport.stream;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

@Service
public class DocumentManagementService {

    private static final String FILES_NAME = "files";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String CLASSIFICATION = "classification";
    private static final String FILES = "files";
    private static final String DOCUMENTS_PATH = "/documents";
    private static final String RESTRICTED = "RESTRICTED";
    private static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";

    private final String dmUri;
    private final RestTemplate restTemplate;
    private final AuthTokenGenerator authTokenGenerator;

    private static final Logger log = LoggerFactory.getLogger(DocumentManagementService.class);

    @Autowired
    public DocumentManagementService(
        AuthTokenGenerator authTokenGenerator,
        RestTemplate restTemplate,
        @Value("${dm.api_gateway.url}") String dmUri
    ) {
        this.authTokenGenerator = authTokenGenerator;
        this.restTemplate = restTemplate;
        this.dmUri = dmUri;
    }

    public List<FileUploadResponse> uploadDocuments(List<Pdf> pdfs) {
        List<MultipartFile> multipartFiles = new ArrayList<>();

        pdfs.forEach(pdf -> {
            MultipartFile file = new InMemoryMultipartFile(
                FILES_NAME,
                pdf.getFilename(),
                Pdf.CONTENT_TYPE,
                pdf.getBytes()
            );

            multipartFiles.add(file);
        });

        HttpEntity<MultiValueMap<String, Object>> httpEntity = new HttpEntity<>(
            prepareRequest(multipartFiles),
            setHttpHeaders()
        );

        try {
            JsonNode documents = restTemplate.postForObject(dmUri + DOCUMENTS_PATH, httpEntity, ObjectNode.class)
                .path("_embedded").path("documents");

            log.info("File upload response from Document Storage service is {}", documents);

            return createFileUploadResponse(documents);

        } catch (Exception exception) {
            log.error("Exception occurred while uploading documents ", exception);
            throw new UnableToUploadDocumentException(exception);
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
            .map(DocumentManagementService::buildPartFromFile)
            .forEach(file -> parameters.add(FILES, file));

        parameters.add(CLASSIFICATION, RESTRICTED);
        return parameters;
    }

    private static HttpEntity<Resource> buildPartFromFile(MultipartFile file) {
        return new HttpEntity<>(buildByteArrayResource(file), buildPartHeaders(file));
    }

    private static HttpHeaders buildPartHeaders(MultipartFile file) {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf(file.getContentType()));
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

    private FileUploadResponse createResponse(JsonNode document) {
        return new FileUploadResponse(
            new HalLinkDiscoverer().findLinkWithRel("self", document.toString()).getHref(),
            document.get("originalDocumentName").asText()
        );
    }

    private List<FileUploadResponse> createFileUploadResponse(JsonNode documents) {
        Stream<JsonNode> filesStream = stream(documents.spliterator(), false);
        return filesStream
            .map(this::createResponse)
            .collect(Collectors.toList());
    }
}
