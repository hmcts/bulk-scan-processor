package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.EnvelopeResponse;

import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class UpdateStatusTest extends BaseFunctionalTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void should_allow_update_envelope_status() throws Exception {
        String destZipFilename = testHelper.getRandomFilename("24-06-2018-00-00-00.test.zip");

        testHelper.uploadZipFile(
            inputContainer,
            Arrays.asList("1111006.pdf", "1111002.pdf"),
            "1111006_2.metadata.json",
            destZipFilename,
            testPrivateKeyDer
        );

        String s2sToken = testHelper.s2sSignIn(this.s2sName, this.s2sSecret, this.s2sUrl);

        waitForFileToBeProcessed(destZipFilename, s2sToken);

        // find our envelope
        UUID envelopeId =
            testHelper
                .getEnvelopeByZipFileName(testUrl, s2sToken, destZipFilename)
                .get()
                .getId();

        testHelper.updateEnvelopeStatus(
            this.testUrl,
            s2sToken,
            envelopeId,
            Status.CONSUMED
        );

        EnvelopeResponse envelopeAfterUpdate = testHelper.getEnvelope(this.testUrl, s2sToken, envelopeId);

        assertThat(envelopeAfterUpdate.getStatus())
            .as("Envelope should have status " + Status.CONSUMED)
            .isEqualTo(Status.CONSUMED);

    }
}
