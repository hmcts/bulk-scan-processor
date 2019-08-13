package uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation;

import com.github.tomakehurst.wiremock.core.Options;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import uk.gov.hmcts.reform.bulkscanprocessor.config.IntegrationContextInitializer;
import uk.gov.hmcts.reform.bulkscanprocessor.config.IntegrationTest;
import uk.gov.hmcts.reform.bulkscanprocessor.config.Profiles;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client.OcrValidationClient;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client.model.req.FormData;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client.model.req.OcrDataField;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client.model.res.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client.model.res.ValidationResponse;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.forbidden;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.unauthorized;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@ActiveProfiles({
    IntegrationContextInitializer.PROFILE_WIREMOCK,
    Profiles.SERVICE_BUS_STUB,
    Profiles.STORAGE_STUB
})
@AutoConfigureWireMock
@IntegrationTest
@RunWith(SpringRunner.class)
public class OcrValidationClientTest {

    @Autowired
    private Options wiremockOptions;

    @Autowired
    private OcrValidationClient client;

    @Test
    public void should_map_error_response_from_service_to_model() {
        // given
        String s2sToken = randomUUID().toString();
        stubFor(
            post(urlPathMatching("/forms/D8/validate-ocr"))
                .withHeader("ServiceAuthorization", equalTo(s2sToken))
                .willReturn(okJson(jsonify(
                    "      {"
                        + "  'status': 'ERRORS',"
                        + "  'errors': ['e1', 'e2'],"
                        + "  'warnings': ['w1', 'w2']"
                        + "}"
                )))
        );

        // when
        ValidationResponse res = client.validate(url(), sampleFormData(), "D8", s2sToken);

        // then
        assertThat(res.status).isEqualTo(Status.ERRORS);
        assertThat(res.errors).contains("e1", "e2");
        assertThat(res.warnings).contains("w1", "w2");
    }

    @Test
    public void should_map_success_response_from_service_to_model() {
        // given
        String s2sToken = randomUUID().toString();
        stubFor(
            post(urlPathMatching("/forms/A1/validate-ocr"))
                .withHeader("ServiceAuthorization", equalTo(s2sToken))
                .willReturn(okJson(jsonify(
                    "      {"
                        + "  'status': 'SUCCESS',"
                        + "  'errors': [],"
                        + "  'warnings': []"
                        + "}"
                )))
        );

        // when
        ValidationResponse res = client.validate(url(), sampleFormData(), "A1", s2sToken);

        // then
        assertThat(res.status).isEqualTo(Status.SUCCESS);
        assertThat(res.errors).isEmpty();
        assertThat(res.warnings).isEmpty();
    }

    @Test
    public void should_map_warnings_response_from_service_to_model() {
        // given
        String s2sToken = randomUUID().toString();
        stubFor(
            post(urlPathMatching("/forms/XY/validate-ocr"))
                .withHeader("ServiceAuthorization", equalTo(s2sToken))
                .willReturn(okJson(jsonify(
                    "      {"
                        + "  'status': 'WARNINGS',"
                        + "  'errors': [],"
                        + "  'warnings': ['w1', 'w2']"
                        + "}"
                )))
        );

        // when
        ValidationResponse res = client.validate(url(), sampleFormData(), "XY", s2sToken);

        // then
        assertThat(res.status).isEqualTo(Status.WARNINGS);
        assertThat(res.errors).isEmpty();
        assertThat(res.warnings).contains("w1", "w2");
    }

    @Test
    public void should_throw_an_exception_if_server_responds_with_an_error_code() {
        // pairs contain server response & expected exception type
        asList(
            Pair.of(serverError(), HttpServerErrorException.InternalServerError.class),
            Pair.of(forbidden(), HttpClientErrorException.Forbidden.class),
            Pair.of(unauthorized(), HttpClientErrorException.Unauthorized.class)
        ).forEach(cfg -> {
            // given
            stubFor(post(urlPathMatching("/forms/.*/validate-ocr")).willReturn(cfg.getLeft()));

            // when
            Throwable err = catchThrowable(
                () -> client.validate(url(), sampleFormData(), "X", randomUUID().toString())
            );

            // then
            assertThat(err).isInstanceOf(cfg.getRight());
        });
    }

    private FormData sampleFormData() {
        return new FormData(
            asList(
                new OcrDataField("name1", "value1"),
                new OcrDataField("name2", "value2")
            )
        );
    }

    private String url() {
        return "http://localhost:" + wiremockOptions.portNumber();
    }

    private String jsonify(String str) {
        return str.replace('\'', '"');
    }
}
