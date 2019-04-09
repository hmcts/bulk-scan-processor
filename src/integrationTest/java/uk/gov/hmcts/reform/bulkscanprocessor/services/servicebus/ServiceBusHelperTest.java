package uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
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
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.InvalidMessageException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.DocumentType;
import uk.gov.hmcts.reform.bulkscanprocessor.model.ocr.OcrData;
import uk.gov.hmcts.reform.bulkscanprocessor.model.ocr.OcrDataField;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.EnvelopeMsg;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.Msg;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.MsgLabel;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.OcrField;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.UUID;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ServiceBusHelperTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Mock
    private IQueueClient queueClient;
    @Mock
    private Envelope envelope;
    @Mock
    private ScannableItem scannableItem1;
    @Mock
    private ScannableItem scannableItem2;

    private ServiceBusHelper serviceBusHelper;

    @Before
    public void setUp() {
        serviceBusHelper = new ServiceBusHelper(queueClient, this.objectMapper);

        mockEnvelopeData();
    }

    @Test
    public void should_send_message_async_with_messageId() {
        Msg msg = new EnvelopeMsg(envelope);
        serviceBusHelper.sendMessageAsync(msg);

        ArgumentCaptor<IMessage> argument = ArgumentCaptor.forClass(IMessage.class);
        verify(queueClient).sendAsync(argument.capture());
        assertThat(argument.getValue())
            .extracting(IMessage::getMessageId)
            .contains(msg.getMsgId());
    }

    @Test
    public void should_send_message_with_messageId() throws Exception {
        Msg msg = new EnvelopeMsg(envelope);
        serviceBusHelper.sendMessage(msg);

        ArgumentCaptor<IMessage> argument = ArgumentCaptor.forClass(IMessage.class);
        verify(queueClient).send(argument.capture());
        assertThat(argument.getValue())
            .extracting(IMessage::getMessageId)
            .contains(msg.getMsgId());
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

    @SuppressWarnings("unchecked")
    @Test
    public void should_send_message_with_envelope_data() throws Exception {

        EnvelopeMsg message = new EnvelopeMsg(envelope);

        serviceBusHelper.sendMessage(message);
        ArgumentCaptor<Message> argument = ArgumentCaptor.forClass(Message.class);
        verify(queueClient).send(argument.capture());

        JsonNode jsonNode = objectMapper.readTree(argument.getValue().getBody());

        assertThat(jsonNode.get("case_ref").textValue()).isEqualTo(message.getCaseNumber());
        assertThat(jsonNode.get("po_box").textValue()).isEqualTo(message.getPoBox());
        assertThat(jsonNode.get("jurisdiction").textValue()).isEqualTo(message.getJurisdiction());
        assertThat(jsonNode.get("container").textValue()).isEqualTo(message.getContainer());
        assertThat(jsonNode.get("zip_file_name").textValue()).isEqualTo(message.getZipFileName());
        assertThat(jsonNode.get("classification").textValue()).isEqualTo(message.getClassification().name());

        assertDateField(jsonNode, "delivery_date", message.getDeliveryDate());
        assertDateField(jsonNode, "opening_date", message.getOpeningDate());

        JsonNode docs = jsonNode.get("documents");
        assertThat(docs.isArray()).isTrue();
        assertThat(docs.size()).isEqualTo(2);
        checkScannableItem(docs.get(0), scannableItem1);
        checkScannableItem(docs.get(1), scannableItem2);

        assertThat(jsonNode.hasNonNull("ocr_data")).isTrue();

        OcrField[] actualOcrData =
            objectMapper.readValue(jsonNode.get("ocr_data").toString(), OcrField[].class);

        OcrField[] expectedOcrData = {
            new OcrField("key1", "value1")
        };

        assertThat(actualOcrData)
            .usingFieldByFieldElementComparator()
            .isEqualTo(expectedOcrData);
    }

    private void mockEnvelopeData() {
        when(envelope.getId()).thenReturn(UUID.randomUUID());
        when(envelope.getCaseNumber()).thenReturn("1111222233334446");
        when(envelope.getPoBox()).thenReturn("SSCS PO BOX");
        when(envelope.getJurisdiction()).thenReturn("SSCS");
        when(envelope.getContainer()).thenReturn("sscs");
        when(envelope.getZipFileName()).thenReturn("zip-file-test.zip");
        when(envelope.getClassification()).thenReturn(Classification.EXCEPTION);
        when(envelope.getDeliveryDate()).thenReturn(Instant.now());
        when(envelope.getOpeningDate()).thenReturn(Instant.now());
        when(envelope.getScannableItems()).thenReturn(Arrays.asList(scannableItem1, scannableItem2));

        when(scannableItem1.getDocumentUrl()).thenReturn("documentUrl1");
        when(scannableItem1.getDocumentControlNumber()).thenReturn("doc1_control_number");
        when(scannableItem1.getFileName()).thenReturn("doc1_file_name");
        when(scannableItem1.getDocumentType()).thenReturn(DocumentType.CHERISHED);
        when(scannableItem1.getScanningDate()).thenReturn(Instant.now());

        OcrData ocrData = new OcrData();
        OcrDataField field = new OcrDataField(new TextNode("key1"), new TextNode("value1"));
        ocrData.setFields(singletonList(field));

        when(scannableItem1.getOcrData()).thenReturn(ocrData);

        when(scannableItem2.getDocumentUrl()).thenReturn("documentUrl2");
        when(scannableItem2.getDocumentControlNumber()).thenReturn("doc2_control_number");
        when(scannableItem2.getFileName()).thenReturn("doc2_file_name");
        when(scannableItem2.getDocumentType()).thenReturn(DocumentType.OTHER);
        when(scannableItem2.getScanningDate()).thenReturn(Instant.now());
        when(scannableItem2.getOcrData()).thenReturn(null);
    }

    @SuppressWarnings("unchecked")
    private void checkScannableItem(JsonNode jsonNode, ScannableItem scannableItem) {
        assertThat(jsonNode.get("file_name").asText()).isEqualTo(scannableItem.getFileName());
        assertThat(jsonNode.get("control_number").asText()).isEqualTo(scannableItem.getDocumentControlNumber());
        assertThat(jsonNode.get("type").asText()).isEqualTo(scannableItem.getDocumentType().toString());
        assertThat(jsonNode.get("url").asText()).isEqualTo(scannableItem.getDocumentUrl());
        assertDateField(jsonNode, "scanned_at", scannableItem.getScanningDate());
    }

    private void assertDateField(JsonNode jsonNode, String field, Instant expectedDate) {
        String iso8601DateTime = ZonedDateTime
            .ofInstant(expectedDate, ZoneId.of("UTC"))
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));

        assertThat(jsonNode.get(field).asText()).isEqualTo(iso8601DateTime);
    }
}
