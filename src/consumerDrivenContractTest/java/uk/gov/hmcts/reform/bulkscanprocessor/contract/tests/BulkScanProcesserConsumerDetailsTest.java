package uk.gov.hmcts.reform.bulkscanprocessor.contract.tests;

import au.com.dius.pact.consumer.Pact;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.model.RequestResponsePact;
import com.google.common.collect.ImmutableList;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.util.ResourceUtils;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client.OcrValidationClient;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client.model.req.FormData;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client.model.req.OcrDataField;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client.model.res.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client.model.res.ValidationResponse;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;

import static io.pactfoundation.consumer.dsl.LambdaDsl.newJsonBody;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;


@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "ocr_validation_service", port = "8889")
@SpringBootTest(properties = {
    "ocr_validation_service: localhost:8889",
    "ocr_validation_service.api.url: localhost:8889"})
@ContextConfiguration(classes = ConsumerApplication.class)
public class BulkScanProcesserConsumerDetailsTest {

    private String s2sToken = randomUUID().toString();
    private String bearerS2sToken = "Bearer " + s2sToken;

    @Autowired
    private OcrValidationClient client;

    @Pact(state = "Consumer POSTS valid OCR data ", provider = "ocr_validation_service",
        consumer = "bulk_scan_processer_service")
    public RequestResponsePact executePostSubmissionOfOcrDataWithSuccessPact(PactDslWithProvider builder) throws
        IOException, JSONException {
        return builder
            .given("BulkscanProcesser POSTS valid ocrData")
            .uponReceiving("a request to POST an valid ocrdata with success response")
            .path("/forms/PERSONAL/validate-ocr")
            .method("POST")
            .headers("ServiceAuthorization", bearerS2sToken)
            .matchHeader("Content-Type", "application/json;charset=UTF-8")
            .body(createJsonObject("ocr_data.json"))
            .willRespondWith()
            .status(200)
            .matchHeader("Content-Type", "application/json")
            .body(newJsonBody(o -> {
                o.array("warnings",a -> {
                    a.nullValue();});
                o.array("errors",a -> {
                    a.nullValue();});
                o.stringValue("status", "SUCCESS");
            }).build())
            .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "executePostSubmissionOfOcrDataWithSuccessPact")
    public void verifyExecutePostSubmissionWithSuccessPact() throws Exception {
        ValidationResponse res = client.validate("http://localhost:8889", getOcrData(), "PERSONAL", s2sToken);
        assertThat(res.status, equalTo(Status.SUCCESS));
    }

    private JSONObject createJsonObject(String fileName) throws JSONException, IOException {
        File file = getFile(fileName);
        String jsonString = new String(Files.readAllBytes(file.toPath()));
        return new JSONObject(jsonString);
    }

    private File getFile(String fileName) throws FileNotFoundException {
        return ResourceUtils.getFile(this.getClass().getResource("/json/" + fileName));
    }

    private FormData getOcrData() throws JSONException, IOException {
        return new FormData(
            ImmutableList.of(
                new OcrDataField("first_name", "John"),
                new OcrDataField("last_name", "Smith"),
                new OcrDataField("date_of_birth", "2000-10-10")
            )
        );
    }

}
