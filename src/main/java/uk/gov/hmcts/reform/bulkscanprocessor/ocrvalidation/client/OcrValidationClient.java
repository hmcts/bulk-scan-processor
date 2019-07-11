package uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client.model.req.FormData;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client.model.res.ValidationResponse;

@Component
public class OcrValidationClient {

    private final RestTemplate restTemplate;

    public OcrValidationClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public ValidationResponse validate(String url, FormData formData, String s2sToken) {

        HttpHeaders headers = new HttpHeaders();
        headers.add("ServiceAuthorization", "Bearer " + s2sToken);

        return restTemplate.postForObject(
            url,
            new HttpEntity<>(formData, headers),
            ValidationResponse.class
        );
    }
}
