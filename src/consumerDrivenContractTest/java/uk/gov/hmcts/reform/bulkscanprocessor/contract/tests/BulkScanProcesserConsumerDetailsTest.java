package uk.gov.hmcts.reform.bulkscanprocessor.contract.tests;

import au.com.dius.pact.consumer.Pact;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.model.RequestResponsePact;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONException;
import org.json.JSONObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.ResourceUtils;

import uk.gov.hmcts.reform.bulkscanprocessor.config.IntegrationTest;
import uk.gov.hmcts.reform.bulkscanprocessor.config.Profiles;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client.OcrValidationClient;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client.model.req.FormData;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client.model.res.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client.model.res.ValidationResponse;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;

import static java.util.UUID.randomUUID;
import static io.pactfoundation.consumer.dsl.LambdaDsl.newJsonBody;


@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "ocr_validation_service", port = "8889")
@IntegrationTest
@SpringBootTest(properties = {
    "ocr_validation_service: localhost:8889",
    "ocr_validation_service.api.url: localhost:8889"
})
public class BulkScanProcesserConsumerDetailsTest {

    public static final String AUTHORIZATION_TOKEN = "someAuthorizationToken";
    public static final String SERVICE_AUTHORIZATION_TOKEN = "Bearer " + randomUUID().toString();

    String s2sToken = "Bearer " + randomUUID().toString();

    @Autowired
    private OcrValidationClient client;

    @Autowired
    ObjectMapper objectMapper;


    @Pact(state = "Consumer POSTS valid ocrdata ", provider = "ocr_validation_service", consumer = "bulk_scan_processer_service")
    public RequestResponsePact executePostSubmissionOfOcrDataWithSuccessPact(PactDslWithProvider builder) throws IOException, JSONException {
        return builder
            .given("BulkscanProcesser POSTS valid ocrData")
            .uponReceiving("a request to POST an valid ocrdata with sucess response")
            .path("/forms/PERSONAL/validate-ocr")
            .method("POST")
            .headers(AUTHORIZATION_TOKEN, SERVICE_AUTHORIZATION_TOKEN)
            .matchHeader("Content-Type", "application/json")
            .body(createJsonObject("ocr_data.json"))
            .willRespondWith()
            .status(200)
            .matchHeader("Content-Type", "application/json;charset=UTF-8")
            .body(newJsonBody((o) -> {
                o.stringValue("warnings", "[]");
                o.numberValue("errors", 0);
                o.stringValue("status", "SUCCESS");
            }).build())
            .toPact();

    }


    @Test
    @PactTestFor(pactMethod = "executePostSubmissionOfOcrDataWithSuccessPact")
    public void verifyExecutePostSubmissionWithSuccessPact() throws Exception {

        ValidationResponse res = client.validate("localhost:8889", getOcrData("ocr_data.json"), "PERSONAL", s2sToken);
        assertThat(res.status,equalTo(Status.SUCCESS));

    }

    private JSONObject createJsonObject(String fileName) throws JSONException, IOException {
        File file = getFile(fileName);
        String jsonString = new String(Files.readAllBytes(file.toPath()));
        return new JSONObject(jsonString);
    }

    private File getFile(String fileName) throws FileNotFoundException {
        return ResourceUtils.getFile(this.getClass().getResource("/json/" + fileName));
    }


    private FormData getOcrData(String fileName) throws JSONException, IOException {
        File file = getFile(fileName);
        FormData ocrData = objectMapper.readValue(file, FormData.class);
        return ocrData;
    }

}
