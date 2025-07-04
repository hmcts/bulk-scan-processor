package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.EnvelopeResponse;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.hmcts.reform.bulkscanprocessor.config.TestConfiguration.FLUX_FUNC_TEST;
import static uk.gov.hmcts.reform.bulkscanprocessor.config.TestConfiguration.PROCESSED_ENVELOPES_QUEUE_CONN_STRING;
import static uk.gov.hmcts.reform.bulkscanprocessor.config.TestConfiguration.PROCESSED_ENVELOPES_QUEUE_NAME;
import static uk.gov.hmcts.reform.bulkscanprocessor.config.TestConfiguration.S2S_NAME;
import static uk.gov.hmcts.reform.bulkscanprocessor.config.TestConfiguration.S2S_SECRET;
import static uk.gov.hmcts.reform.bulkscanprocessor.config.TestConfiguration.S2S_URL;
import static uk.gov.hmcts.reform.bulkscanprocessor.config.TestConfiguration.SCAN_DELAY;
import static uk.gov.hmcts.reform.bulkscanprocessor.config.TestConfiguration.STORAGE_CONTAINER_NAME;
import static uk.gov.hmcts.reform.bulkscanprocessor.config.TestConfiguration.TEST_URL;

/**
 * If running locally (local machine), use the JmsProcessedEnvelopeHandlingTest.
 */
public class ProcessedEnvelopeMessageHandlingTest extends BaseFunctionalTest {

    private static final long MESSAGE_PROCESSING_TIMEOUT_MILLIS = 60_000;
    private static final long ENVELOPE_FINALISATION_TIMEOUT_MILLIS = 60_000;
    private static final int DELETE_TIMEOUT_MILLIS = 60_000;
    private static final List<Status> COMPLETED_OR_NOTIFICATION_SENT = List.of(
        Status.NOTIFICATION_SENT,
        Status.COMPLETED
    );

    private String s2sToken;
    private ServiceBusSenderClient queueSendClient;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        this.s2sToken = testHelper.s2sSignIn(S2S_NAME, S2S_SECRET, S2S_URL);
        this.queueSendClient = getSendClient();
    }

//    @Test
//    public void should_complete_envelope_referenced_by_queue_message() {
//        // given
//        var zipFilename = uploadEnvelope(asList("1111006.pdf", "1111002.pdf"), "exception_with_ocr_metadata.json");
//
//        assertZipFileContentProcessed(zipFilename, 2);
//    }

//    @Test
//    public void should_complete_envelope_with_new_document_type() {
//        // given
//        var zipFilename = uploadEnvelope(singletonList("1111006.pdf"), "exception_metadata.json");
//
//        assertZipFileContentProcessed(zipFilename, 1);
//    }

    private void assertZipFileContentProcessed(String zipFilename, int expectedDocumentsSize) {
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
        assertThat(updatedEnvelope.getScannableItems()).hasSize(expectedDocumentsSize);

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
        var envelope = testHelper.getEnvelopeByContainerAndFileName(TEST_URL, STORAGE_CONTAINER_NAME, zipFilename);
        return envelope != null && COMPLETED_OR_NOTIFICATION_SENT.contains(envelope.getStatus());
        // Ideally, we'd like to wait until the envelope is in NOTIFICATION_SENT status,
        // but there's a risk of it becoming completed if there's an orchestrator working
        // in the same environment.
    }

    private String uploadEnvelope(List<String> documents, String fileName) {
        String zipFilename = testHelper.getRandomFilename();

        testHelper.uploadZipFile(
            inputContainer,
            documents,
            fileName,
            zipFilename
        );

        return zipFilename;
    }

    private EnvelopeResponse getEnvelope(String zipFilename) {
        return testHelper.getEnvelopeByContainerAndFileName(TEST_URL, STORAGE_CONTAINER_NAME, zipFilename);
    }

    //unknown fields should be ignored
    private void sendProcessedEnvelopeMessage(UUID envelopeId, String ccdId, String ccdAction) {
        ServiceBusMessage message = new ServiceBusMessage(
            " {"
                + "\"envelope_id\":\"" + envelopeId + "\","
                + "\"ccd_id\":\"" + ccdId + "\","
                + "\"envelope_ccd_action\":\"" + ccdAction + "\","
                + "\"dummy\":\"value-should-ignore\""
                + "}"
        );

        queueSendClient.sendMessage(message);
    }

    private ServiceBusSenderClient getSendClient() {
        return new ServiceBusClientBuilder()
            .connectionString(PROCESSED_ENVELOPES_QUEUE_CONN_STRING)
            .sender()
            .queueName(PROCESSED_ENVELOPES_QUEUE_NAME)
            .buildClient();

    }
}

