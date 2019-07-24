package uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client.model.req.FormData;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client.model.res.ValidationResponse;

@Component
public class OcrValidationClient {

    public static final String PATH = "/forms/{form-type}/validate-ocr";

    private final RestTemplate restTemplate;

    public OcrValidationClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public ValidationResponse validate(String baseUrl, FormData formData, String s2sToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("ServiceAuthorization", "Bearer " + s2sToken);

        String url =
            UriComponentsBuilder
                .fromHttpUrl(baseUrl)
                .path(PATH)
                .buildAndExpand(formData.type)
                .toString();

        return restTemplate.postForObject(
            url,
            new HttpEntity<>(formData, headers),
            ValidationResponse.class
        );
    }
}
