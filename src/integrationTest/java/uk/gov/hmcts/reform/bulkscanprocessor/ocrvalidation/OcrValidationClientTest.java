package uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.config.IntegrationContextInitializer;
import uk.gov.hmcts.reform.bulkscanprocessor.config.IntegrationTest;
import uk.gov.hmcts.reform.bulkscanprocessor.config.Profiles;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.model.req.FormData;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.model.req.OcrDataField;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.model.res.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.model.res.ValidationResponse;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

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
    private OcrValidationClient client;

    @Test
    public void should_map_response_from_service_to_model() {
        // given
        String responseJson = "{"
            + "'status' : 'ERRORS',"
            + "'errors': ['e1', 'e2'],"
            + "'warnings': '['w1', 'w2']'"
            + "}";

        stubFor(post("/validate-ocr").willReturn(okJson(responseJson)));

        // when
        ValidationResponse response = client.validate(
            "localhost",
            new FormData(
                "type",
                asList(
                    new OcrDataField("key1", "value1"),
                    new OcrDataField("key2", "value2")
                )
            )
        );

        // then
        assertThat(response.errors).contains("e1", "e2");
        assertThat(response.warnings).contains("w1", "w2");
        assertThat(response.status).isEqualTo(Status.ERRORS);
    }

    @Test
    public void should_throw_an_exception() {
    }
}
