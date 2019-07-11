package uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation;

import com.github.tomakehurst.wiremock.core.Options;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.HttpServerErrorException.InternalServerError;
import uk.gov.hmcts.reform.bulkscanprocessor.config.IntegrationContextInitializer;
import uk.gov.hmcts.reform.bulkscanprocessor.config.IntegrationTest;
import uk.gov.hmcts.reform.bulkscanprocessor.config.Profiles;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.model.req.FormData;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.model.req.OcrDataField;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.model.res.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.model.res.ValidationResponse;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@ActiveProfiles( {
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
    public void should_map_response_from_service_to_model() {
        // given
        stubFor(
            post("/validate-ocr")
                .willReturn(okJson(jsonify(
                    "      {"
                        + "  'status': 'ERRORS',"
                        + "  'errors': ['e1', 'e2'],"
                        + "  'warnings': ['w1', 'w2']"
                        + "}"
                )))
        );

        // when
        ValidationResponse res = client.validate(url(), sampleFormData());

        // then
        assertThat(res.errors).contains("e1", "e2");
        assertThat(res.warnings).contains("w1", "w2");
        assertThat(res.status).isEqualTo(Status.ERRORS);
    }

    @Test
    public void should_throw_an_exception_if_server_responds_with_an_error_code() {
        // given
        stubFor(
            post("/validate-ocr")
                .willReturn(serverError())
        );

        // when
        Throwable err = catchThrowable(() -> client.validate(url(), sampleFormData()));

        // then
        assertThat(err).isInstanceOf(InternalServerError.class);
    }

    private FormData sampleFormData() {
        return new FormData(
            "type",
            asList(
                new OcrDataField("name1", "value1"),
                new OcrDataField("name2", "value2")
            )
        );
    }

    private String url() {
        return "http://localhost:" + wiremockOptions.portNumber() + "/validate-ocr";
    }

    private String jsonify(String s) {
        return s.replace('\'', '"');
    }
}
