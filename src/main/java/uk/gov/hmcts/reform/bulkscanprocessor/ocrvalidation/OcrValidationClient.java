package uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation;

import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.model.req.FormData;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.model.res.ValidationResponse;

@Component
public class OcrValidationClient {

    private final RestTemplate restTemplate;

    public OcrValidationClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public ValidationResponse validate(String url, FormData formData) {
        return restTemplate.postForObject(
            url,
            new HttpEntity<>(formData),
            ValidationResponse.class
        );
    }
}
