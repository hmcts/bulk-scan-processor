package uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.IQueueClient;
import com.microsoft.azure.servicebus.Message;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Classification;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.InvalidMessageException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.EnvelopeMsg;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.Msg;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.MsgLabel;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ServiceBusHelperTest {

    @Autowired private ObjectMapper objectMapper;

    @Mock private IQueueClient queueClient;
    @Mock private QueueClientSupplier queueClientSupplier;
    @Mock private Envelope envelope;
    @Mock private ScannableItem scannableItem1;
    @Mock private ScannableItem scannableItem2;

    private ServiceBusHelper serviceBusHelper;

    @Before
    public void setUp() {
        when(queueClientSupplier.get()).thenReturn(this.queueClient);
        serviceBusHelper = new ServiceBusHelper(queueClientSupplier, this.objectMapper);

        mockEnvelopeData();
    }

    @Test
    public void should_send_message_async_with_messageId() {
        Msg msg = new EnvelopeMsg(envelope);
        serviceBusHelper.sendMessageAsync(msg);

        ArgumentCaptor<IMessage> argument = ArgumentCaptor.forClass(IMessage.class);
        verify(queueClient).sendAsync(argument.capture());
        assertThat(argument.getValue())
            .extracting(IMessage::getMessageId).containsExactly(msg.getMsgId());
    }

    @Test
    public void should_send_message_with_messageId() throws Exception {
        Msg msg = new EnvelopeMsg(envelope);
        serviceBusHelper.sendMessage(msg);

        ArgumentCaptor<IMessage> argument = ArgumentCaptor.forClass(IMessage.class);
        verify(queueClient).send(argument.capture());
        assertThat(argument.getValue())
            .extracting(IMessage::getMessageId).containsExactly(msg.getMsgId());
    }

    @Test(expected = InvalidMessageException.class)
    public void should_throw_exception_for_empty_messageId() {
        when(envelope.getId()).thenReturn(null);
        Msg msg = new EnvelopeMsg(envelope);
        serviceBusHelper.sendMessage(msg);
    }

    @Test
    public void should_add_test_label_to_test_message() {
        when(envelope.isTestOnly()).thenReturn(true);
        Msg msg = new EnvelopeMsg(envelope);
        Message busMessage = serviceBusHelper.mapToBusMessage(msg);
        assertThat(busMessage.getLabel()).isEqualTo(MsgLabel.TEST.toString());
    }

    @Test
    public void should_not_add_any_label_to_standard_message() {
        Msg msg = new EnvelopeMsg(envelope);
        Message busMessage = serviceBusHelper.mapToBusMessage(msg);
        assertThat(busMessage.getLabel()).isNullOrEmpty();
    }

    @Test
    public void should_send_message_with_envelope_data() throws Exception {

        EnvelopeMsg message = new EnvelopeMsg(envelope);

        serviceBusHelper.sendMessage(message);
        ArgumentCaptor<Message> argument = ArgumentCaptor.forClass(Message.class);
        verify(queueClient).send(argument.capture());

        JsonNode jsonNode = objectMapper.readTree(argument.getValue().getBody());

        assertThat(jsonNode.get("case_ref").textValue()).isEqualTo(message.getCaseNumber());
        assertThat(jsonNode.get("jurisdiction").textValue()).isEqualTo(message.getJurisdiction());
        assertThat(jsonNode.get("zip_file_name").textValue()).isEqualTo(message.getZipFileName());
        assertThat(jsonNode.get("classification").textValue()).isEqualTo(message.getClassification().name());
        assertThat(jsonNode.get("delivery_date").textValue())
            .isEqualTo(objectMapper.writeValueAsString(message.getDeliveryDate()).replace("\"", ""));
        assertThat(jsonNode.get("opening_date").textValue())
            .isEqualTo(objectMapper.writeValueAsString(message.getOpeningDate()).replace("\"", ""));

        JsonNode docUrls = jsonNode.get("doc_urls");
        assertThat(docUrls.isArray()).isTrue();
        assertThat(docUrls.size()).isEqualTo(2);
        assertThat(docUrls.get(0).asText()).isEqualTo(scannableItem1.getDocumentUrl());
        assertThat(docUrls.get(1).asText()).isEqualTo(scannableItem2.getDocumentUrl());

        JsonNode docs = jsonNode.get("documents");
        assertThat(docs.isArray()).isTrue();
        assertThat(docs.size()).isEqualTo(2);
        checkScannableItem(docs.get(0), scannableItem1);
        checkScannableItem(docs.get(1), scannableItem2);
    }

    private void mockEnvelopeData() {
        when(envelope.getId()).thenReturn(UUID.randomUUID());
        when(envelope.getCaseNumber()).thenReturn("1111222233334446");
        when(envelope.getJurisdiction()).thenReturn("SSCS");
        when(envelope.getZipFileName()).thenReturn("zip-file-test.zip");
        when(envelope.getClassification()).thenReturn(Classification.EXCEPTION);
        when(envelope.getDeliveryDate()).thenReturn(Timestamp.valueOf("2010-10-10 10:10:10.101010"));
        when(envelope.getOpeningDate()).thenReturn(Timestamp.valueOf("2010-10-10 10:10:10.101010"));
        when(envelope.getScannableItems()).thenReturn(Arrays.asList(scannableItem1, scannableItem2));

        when(scannableItem1.getDocumentUrl()).thenReturn("documentUrl1");
        when(scannableItem1.getDocumentControlNumber()).thenReturn("doc1_control_number");
        when(scannableItem1.getFileName()).thenReturn("doc1_file_name");
        when(scannableItem1.getDocumentType()).thenReturn("doc1_type");
        when(scannableItem1.getScanningDate()).thenReturn(Timestamp.from(Instant.now()));

        when(scannableItem2.getDocumentUrl()).thenReturn("documentUrl2");
        when(scannableItem2.getDocumentControlNumber()).thenReturn("doc2_control_number");
        when(scannableItem2.getFileName()).thenReturn("doc2_file_name");
        when(scannableItem2.getDocumentType()).thenReturn("doc2_type");
        when(scannableItem2.getScanningDate()).thenReturn(Timestamp.from(Instant.now()));
    }

    private void checkScannableItem(JsonNode jsonNode, ScannableItem scannableItem) {
        assertThat(jsonNode.get("file_name").asText()).isEqualTo(scannableItem.getFileName());
        assertThat(jsonNode.get("control_number").asText()).isEqualTo(scannableItem.getDocumentControlNumber());
        assertThat(jsonNode.get("type").asText()).isEqualTo(scannableItem.getDocumentType());
        assertThat(jsonNode.get("url").asText()).isEqualTo(scannableItem.getDocumentUrl());

        String iso8601DateTime =
            ZonedDateTime
                .ofInstant(scannableItem.getScanningDate().toInstant(), ZoneId.of("UTC"))
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));

        assertThat(jsonNode.get("scanned_at").asText()).isEqualTo(iso8601DateTime);
    }
}
