package uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client.model.req.FormData;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client.model.res.ValidationResponse;

/**
 * Client for OCR validation service.
 */
@Component
public class OcrValidationClient {

    private final RestTemplate restTemplate;

    /**
     * Constructor for OcrValidationClient.
     * @param restTemplate RestTemplate
     */
    public OcrValidationClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Validates the OCR data.
     * @param baseUrl base URL of the OCR validation service
     * @param formData OCR data to validate
     * @param formType form type
     * @param s2sToken S2S token
     * @return validation response
     */
    public ValidationResponse validate(
        String baseUrl,
        FormData formData,
        String formType,
        String s2sToken
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("ServiceAuthorization", s2sToken);
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        String url =
            UriComponentsBuilder
                .fromHttpUrl(baseUrl)
                .path("/forms/{form-type}/validate-ocr")
                .buildAndExpand(formType)
                .toString();

        return restTemplate.postForObject(
            url,
            new HttpEntity<>(formData, headers),
            ValidationResponse.class
        );
    }
}
