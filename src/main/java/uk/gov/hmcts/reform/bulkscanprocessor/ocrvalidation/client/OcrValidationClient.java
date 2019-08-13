package uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client.model.req.FormData;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client.model.res.ValidationResponse;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;

@Component
public class OcrValidationClient {

    private final RestTemplate restTemplate;

    public OcrValidationClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.restTemplate.setInterceptors(Collections.singletonList(new Interceptor()));
    }

    public ValidationResponse validate(
        String baseUrl,
        FormData formData,
        String formType,
        String s2sToken
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("ServiceAuthorization", s2sToken);

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

    private static class Interceptor implements ClientHttpRequestInterceptor {

        private static final Logger log = LoggerFactory.getLogger(Interceptor.class);

        @Override
        public ClientHttpResponse intercept(
            HttpRequest req,
            byte[] body,
            ClientHttpRequestExecution execution
        ) throws IOException {
            log.info(
                "URI: {}, Headers: {}, Body: {}",
                req.getURI(),
                req.getHeaders(),
                new String(body)
            );

            ClientHttpResponse resp = execution.execute(req, body);

            log.info(
                "Status: {}, Body: {}",
                resp.getRawStatusCode(),
                StreamUtils.copyToString(resp.getBody(), Charset.defaultCharset())
            );

            return resp;
        }
    }
}
