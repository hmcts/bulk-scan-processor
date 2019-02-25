package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.Message;
import com.microsoft.azure.servicebus.QueueClient;
import com.microsoft.azure.servicebus.ReceiveMode;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.model.in.msg.ProcessedEnvelope;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.EnvelopeResponse;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;

public class ProcessedEnvelopeMessageHandlingTest extends BaseFunctionalTest {

    private static final long MAX_MESSAGE_PROCESSING_TIME_MILLIS = 40_000;
    private static final long MAX_ENVELOPE_FINALISATION_TIME_MILLIS = 10_000;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private String s2sToken;
    private QueueClient queueClient;

    @Before
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
        String zipFilename = uploadEnvelopeContainingOcrData();

        await("Envelope should be created in the service")
            .atMost(scanDelay + MAX_MESSAGE_PROCESSING_TIME_MILLIS, TimeUnit.MILLISECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .until(() -> testHelper.getEnvelopeByZipFileName(testUrl, s2sToken, zipFilename).isPresent());

        UUID envelopeId = getEnvelope(zipFilename).getId();

        // when
        sendProcessedEnvelopeMessage(envelopeId);

        // then
        await("Envelope should change status to 'COMPLETED'")
            .atMost(MAX_ENVELOPE_FINALISATION_TIME_MILLIS, TimeUnit.MILLISECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .until(() -> getEnvelope(zipFilename).getStatus() == Status.COMPLETED);

        EnvelopeResponse updatedEnvelope = getEnvelope(zipFilename);
        assertThat(updatedEnvelope.getScannableItems()).hasSize(2);
        assertThat(updatedEnvelope.getScannableItems()).allMatch(item -> item.ocrData == null);
        assertThat(updatedEnvelope.getStatus()).isEqualTo(Status.COMPLETED);
    }

    private String uploadEnvelopeContainingOcrData() throws Exception {
        String zipFilename = testHelper.getRandomFilename("12-02-2019-00-00-00.test.zip");

        testHelper.uploadZipFile(
            inputContainer,
            Arrays.asList("1111006.pdf", "1111002.pdf"),
            "1111007.metadata-with-ocr-data.json",
            zipFilename,
            testPrivateKeyDer
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

    private void sendProcessedEnvelopeMessage(UUID envelopeId) throws Exception {
        IMessage message = new Message(
            objectMapper.writeValueAsString(new ProcessedEnvelope(envelopeId))
        );

        queueClient.send(message);
    }
}
