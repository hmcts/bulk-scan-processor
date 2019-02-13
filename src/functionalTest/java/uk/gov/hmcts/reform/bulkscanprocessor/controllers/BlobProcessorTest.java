package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import org.assertj.core.util.Strings;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.EnvelopeResponse;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;

public class BlobProcessorTest extends BaseFunctionalTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void should_process_zipfile_after_upload_and_set_status() throws Exception {
        List<String> files = Arrays.asList("1111006.pdf", "1111002.pdf");
        String metadataFile = "1111006_2.metadata.json";
        String destZipFilename = testHelper.getRandomFilename("24-06-2018-00-00-00.test.zip");

        // valid zip file
        testHelper.uploadZipFile(inputContainer, files, metadataFile, destZipFilename, testPrivateKeyDer);


        String s2sToken = testHelper.s2sSignIn(this.s2sName, this.s2sSecret, this.s2sUrl);

        await("processing should end")
            .atMost(scanDelay + 40_000, TimeUnit.MILLISECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .until(() -> testHelper.getEnvelopeByZipFileName(testUrl, s2sToken, destZipFilename)
                .filter(env -> env.getStatus() == Status.NOTIFICATION_SENT)
                .isPresent()
            );

        EnvelopeResponse envelope = testHelper.getEnvelopeByZipFileName(testUrl, s2sToken, destZipFilename).get();

        assertThat(envelope.getStatus()).isEqualTo(Status.NOTIFICATION_SENT);
        assertThat(envelope.getScannableItems()).hasSize(2);
        assertThat(envelope.getScannableItems()).noneMatch(item -> Strings.isNullOrEmpty(item.documentUrl));
    }
}
