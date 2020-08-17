package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import com.google.common.collect.ImmutableList;
import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.Message;
import com.microsoft.azure.servicebus.QueueClient;
import com.microsoft.azure.servicebus.ReceiveMode;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.EnvelopeResponse;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

public class ProcessedEnvelopeMessageHandlingTest extends BaseFunctionalTest {

    private static final long MESSAGE_PROCESSING_TIMEOUT_MILLIS = 40_000;
    private static final long ENVELOPE_FINALISATION_TIMEOUT_MILLIS = 40_000;
    private static final int DELETE_TIMEOUT_MILLIS = 40_000;

    private String s2sToken;
    private QueueClient queueClient;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        this.s2sToken = testHelper.s2sSignIn(this.s2sName, this.s2sSecret, this.s2sUrl);

        this.queueClient = new QueueClient(
            new ConnectionStringBuilder(
                config.getString("processed-envelopes-queue-conn-string")
            ),
            ReceiveMode.PEEKLOCK
        );
    }

    @Test
    public void should_complete_envelope_referenced_by_queue_message() throws Exception {
        // given
        String zipFilename = uploadEnvelope();

        await(
            "File " + zipFilename + " should be created in the service and notification should be put on the queue"
        )
            .atMost(scanDelay + MESSAGE_PROCESSING_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .until(() -> hasNotificationBeenSent(zipFilename));

        UUID envelopeId = getEnvelope(zipFilename).getId();

        // when
        String ccdAction = fluxFuncTest ? "EXCEPTION_RECORD" : "ccd-action";
        if (!fluxFuncTest) {
            sendProcessedEnvelopeMessage(envelopeId, "ccd-id", ccdAction);
        }
        // then
        await("File " + zipFilename + " should change status to 'COMPLETED'")
            .atMost(ENVELOPE_FINALISATION_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .until(() -> getEnvelope(zipFilename).getStatus() == Status.COMPLETED);

        EnvelopeResponse updatedEnvelope = getEnvelope(zipFilename);
        assertThat(updatedEnvelope.getScannableItems()).hasSize(2);
        assertThat(updatedEnvelope.getScannableItems()).allMatch(item -> item.ocrData == null);

        if (fluxFuncTest) {
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
        return testHelper.getEnvelopeByZipFileName(testUrl, s2sToken, zipFilename)
            .map(envelope ->
                // Ideally, we'd like to wait until the envelope is in NOTIFICATION_SENT status,
                // but there's a risk of it becoming completed if there's an orchestrator working
                // in the same environment.
                ImmutableList.of(Status.NOTIFICATION_SENT, Status.COMPLETED)
                    .contains(envelope.getStatus())
            )
            .orElse(false);
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
        return testHelper.getEnvelopeByZipFileName(testUrl, s2sToken, zipFilename)
            .orElseThrow(() ->
                new AssertionError(
                    String.format("The envelope for file %s couldn't be found", zipFilename)
                )
            );
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

