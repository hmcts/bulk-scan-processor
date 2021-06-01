package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import com.google.common.collect.ImmutableList;
import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.Message;
import com.microsoft.azure.servicebus.QueueClient;
import com.microsoft.azure.servicebus.ReceiveMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.EnvelopeResponse;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.hmcts.reform.bulkscanprocessor.config.TestConfiguration.FLUX_FUNC_TEST;
import static uk.gov.hmcts.reform.bulkscanprocessor.config.TestConfiguration.PROCESSED_ENVELOPES_QUEUE_CONN_STRING_BUILDER;
import static uk.gov.hmcts.reform.bulkscanprocessor.config.TestConfiguration.S2S_NAME;
import static uk.gov.hmcts.reform.bulkscanprocessor.config.TestConfiguration.S2S_SECRET;
import static uk.gov.hmcts.reform.bulkscanprocessor.config.TestConfiguration.S2S_URL;
import static uk.gov.hmcts.reform.bulkscanprocessor.config.TestConfiguration.SCAN_DELAY;
import static uk.gov.hmcts.reform.bulkscanprocessor.config.TestConfiguration.STORAGE_CONTAINER_NAME;
import static uk.gov.hmcts.reform.bulkscanprocessor.config.TestConfiguration.TEST_URL;

public class ProcessedEnvelopeMessageHandlingTest extends BaseFunctionalTest {

    private static final long MESSAGE_PROCESSING_TIMEOUT_MILLIS = 60_000;
    private static final long ENVELOPE_FINALISATION_TIMEOUT_MILLIS = 60_000;
    private static final int DELETE_TIMEOUT_MILLIS = 60_000;
    private static List COMPLETED_OR_NOTIFICATION_SENT = ImmutableList.of(Status.NOTIFICATION_SENT, Status.COMPLETED);

    private String s2sToken;
    private QueueClient queueClient;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        this.s2sToken = testHelper.s2sSignIn(S2S_NAME, S2S_SECRET, S2S_URL);

        this.queueClient = new QueueClient(
            PROCESSED_ENVELOPES_QUEUE_CONN_STRING_BUILDER,
            ReceiveMode.PEEKLOCK
        );
    }

    @Test
    public void should_complete_envelope_referenced_by_queue_message() throws Exception {
        // given
        var zipFilename = uploadEnvelope();

        await(
            "File " + zipFilename + " should be created in the service and notification should be put on the queue"
        )
            .atMost(SCAN_DELAY + MESSAGE_PROCESSING_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .until(() -> hasNotificationBeenSent(zipFilename));

        UUID envelopeId = getEnvelope(zipFilename).getId();

        // when
        var ccdAction = FLUX_FUNC_TEST ? "EXCEPTION_RECORD" : "ccd-action";
        if (!FLUX_FUNC_TEST) {
            sendProcessedEnvelopeMessage(envelopeId, "ccd-id", ccdAction);
        }
        // then
        await("File " + zipFilename + " should change status to 'COMPLETED'")
            .atMost(ENVELOPE_FINALISATION_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .until(() -> getEnvelope(zipFilename).getStatus() == Status.COMPLETED);

        var updatedEnvelope = getEnvelope(zipFilename);
        assertThat(updatedEnvelope.getScannableItems()).hasSize(2);

        if (FLUX_FUNC_TEST) {
            assertThat(updatedEnvelope.getCcdId()).isNotEmpty();
        } else {
            assertThat(updatedEnvelope.getCcdId()).isEqualTo("ccd-id");
        }
        assertThat(updatedEnvelope.getEnvelopeCcdAction()).isEqualTo(ccdAction);


        await("File " + zipFilename + " should be deleted")
            .atMost(DELETE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
            .pollInterval(2, TimeUnit.SECONDS)
            .until(() -> testHelper.storageHasFile(inputContainer, zipFilename), is(false));
    }

    private Boolean hasNotificationBeenSent(String zipFilename) {
        return
            COMPLETED_OR_NOTIFICATION_SENT.contains(
                testHelper.getEnvelopeByContainerAndFileName(TEST_URL, STORAGE_CONTAINER_NAME, zipFilename)
                    .getStatus());
        // Ideally, we'd like to wait until the envelope is in NOTIFICATION_SENT status,
        // but there's a risk of it becoming completed if there's an orchestrator working
        // in the same environment.
    }

    private String uploadEnvelope() {
        String zipFilename = testHelper.getRandomFilename();

        testHelper.uploadZipFile(
            inputContainer,
            Arrays.asList("1111006.pdf", "1111002.pdf"),
            "exception_with_ocr_metadata.json",
            zipFilename
        );

        return zipFilename;
    }

    private EnvelopeResponse getEnvelope(String zipFilename) {
        return testHelper.getEnvelopeByContainerAndFileName(TEST_URL, STORAGE_CONTAINER_NAME, zipFilename);
    }

    //unknown fields should be ignored
    private void sendProcessedEnvelopeMessage(UUID envelopeId, String ccdId, String ccdAction) throws Exception {
        IMessage message = new Message(
            " {"
                + "\"envelope_id\":\"" + envelopeId + "\","
                + "\"ccd_id\":\"" + ccdId + "\","
                + "\"envelope_ccd_action\":\"" + ccdAction + "\","
                + "\"dummy\":\"value-should-ignore\""
                + "}"
        );

        queueClient.send(message);
    }
}

