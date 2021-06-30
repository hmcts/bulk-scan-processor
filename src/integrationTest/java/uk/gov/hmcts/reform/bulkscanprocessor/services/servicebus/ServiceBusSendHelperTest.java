package uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus;

import com.azure.messaging.servicebus.ServiceBusException;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import uk.gov.hmcts.reform.bulkscanprocessor.config.IntegrationTest;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Payment;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.InvalidMessageException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.DocumentSubtype;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.DocumentType;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.OcrData;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.OcrDataField;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.EnvelopeMsg;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.Msg;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.MsgLabel;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.OcrField;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.UUID;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@EnableJpaRepositories(basePackages = "uk.gov.hmcts.reform.bulkscanprocessor.entity")
@IntegrationTest
public class ServiceBusSendHelperTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Mock
    private ServiceBusSenderClient sendClient;

    @Mock
    private Envelope envelope;

    @Mock
    private ScannableItem scannableItem1;

    @Mock
    private ScannableItem scannableItem2;

    @Mock
    private Payment payment1;

    @Mock
    private Payment payment2;

    private ServiceBusSendHelper serviceBusHelper;


    @BeforeEach
    public void setUp() {
        serviceBusHelper = new ServiceBusSendHelper(sendClient, objectMapper);

        mockEnvelopeData();
    }

    @Test
    public void should_send_message_with_messageId() throws Exception {
        Msg msg = new EnvelopeMsg(envelope);
        serviceBusHelper.sendMessage(msg);

        ArgumentCaptor<ServiceBusMessage> argument = ArgumentCaptor.forClass(ServiceBusMessage.class);
        verify(sendClient).sendMessage(argument.capture());
        assertThat(argument.getValue())
            .extracting(ServiceBusMessage::getMessageId)
            .asString()
            .contains(msg.getMsgId());
    }

    @Test
    public void should_throw_exception_for_empty_messageId() {
        when(envelope.getId()).thenReturn(null);
        Msg msg = new EnvelopeMsg(envelope);
        assertThatCode(() -> serviceBusHelper.sendMessage(msg))
            .isInstanceOf(InvalidMessageException.class);
    }

    @Test
    public void should_add_test_label_to_test_message() {
        when(envelope.isTestOnly()).thenReturn(true);
        Msg msg = new EnvelopeMsg(envelope);
        ServiceBusMessage busMessage = serviceBusHelper.mapToBusMessage(msg);
        assertThat(busMessage.getSubject()).isEqualTo(MsgLabel.TEST.toString());
    }

    @Test
    public void should_not_add_any_label_to_standard_message() {
        Msg msg = new EnvelopeMsg(envelope);
        ServiceBusMessage busMessage = serviceBusHelper.mapToBusMessage(msg);
        assertThat(busMessage.getSubject()).isNullOrEmpty();
    }

    @Test
    public void should_throw_exception_when_message_null() {
        Throwable exc = catchThrowable(() -> serviceBusHelper.mapToBusMessage(null));
        //then
        assertThat(exc)
            .isInstanceOf(InvalidMessageException.class)
            .hasMessage("Msg == null");
    }

    @Test
    public void should_throw_exception_when_service_bus_connection_times_out() throws Exception {
        // given
        Msg msg = new EnvelopeMsg(envelope);
        willThrow(ServiceBusException.class).given(sendClient).sendMessage(any());

        // when
        Throwable exc = catchThrowable(() -> serviceBusHelper.sendMessage(msg));

        //then
        assertThat(exc)
            .isInstanceOf(InvalidMessageException.class)
            .hasMessage("Unable to send message");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void should_send_message_with_envelope_data() throws Exception {

        EnvelopeMsg message = new EnvelopeMsg(envelope);

        serviceBusHelper.sendMessage(message);
        ArgumentCaptor<ServiceBusMessage> argument = ArgumentCaptor.forClass(ServiceBusMessage.class);
        verify(sendClient).sendMessage(argument.capture());

        JsonNode jsonNode = objectMapper.readTree(argument.getValue().getBody().toBytes());

        assertThat(jsonNode.get("case_ref").textValue()).isEqualTo(message.getCaseNumber());
        assertThat(jsonNode.get("previous_service_case_ref").textValue())
            .isEqualTo(message.getPreviousServiceCaseReference());

        assertThat(jsonNode.get("po_box").textValue()).isEqualTo(message.getPoBox());
        assertThat(jsonNode.get("jurisdiction").textValue()).isEqualTo(message.getJurisdiction());
        assertThat(jsonNode.get("container").textValue()).isEqualTo(message.getContainer());
        assertThat(jsonNode.get("zip_file_name").textValue()).isEqualTo(message.getZipFileName());
        assertThat(jsonNode.get("classification").textValue()).isEqualTo(message.getClassification().name());
        assertThat(jsonNode.get("form_type").textValue()).isEqualTo(DocumentSubtype.SSCS1);

        JsonNode ocrDataValidationWarnings = jsonNode.get("ocr_data_validation_warnings");
        assertThat(ocrDataValidationWarnings.isArray()).isTrue();
        assertThat(ocrDataValidationWarnings.elements())
            .toIterable()
            .containsExactly(new TextNode("warning 1"));

        assertDateField(jsonNode, "delivery_date", message.getDeliveryDate());
        assertDateField(jsonNode, "opening_date", message.getOpeningDate());

        assertMessageContainsRightDocuments(jsonNode);
        assertMessageContainsRightPayments(jsonNode);

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
        when(envelope.getPreviousServiceCaseReference()).thenReturn("12345678");
        when(envelope.getPoBox()).thenReturn("SSCS PO BOX");
        when(envelope.getJurisdiction()).thenReturn("SSCS");
        when(envelope.getContainer()).thenReturn("sscs");
        when(envelope.getZipFileName()).thenReturn("zip-file-test.zip");
        when(envelope.getClassification()).thenReturn(Classification.EXCEPTION);
        when(envelope.getDeliveryDate()).thenReturn(Instant.now());
        //2021-04-16T09:01:43.029Z (not with 6 digit millisecond)
        LocalDateTime dateTime = LocalDateTime.parse("2021-04-16T09:01:43.029000");
        when(envelope.getOpeningDate()).thenReturn(dateTime.toInstant(ZoneOffset.UTC));
        when(envelope.getScannableItems()).thenReturn(Arrays.asList(scannableItem1, scannableItem2));
        when(envelope.getPayments()).thenReturn(Arrays.asList(payment1, payment2));

        when(scannableItem1.getDocumentUuid()).thenReturn("documentUuid1");
        when(scannableItem1.getDocumentControlNumber()).thenReturn("doc1_control_number");
        when(scannableItem1.getFileName()).thenReturn("doc1_file_name");
        when(scannableItem1.getDocumentType()).thenReturn(DocumentType.CHERISHED);
        when(scannableItem1.getScanningDate()).thenReturn(Instant.now());

        OcrData ocrData = new OcrData(singletonList(
            new OcrDataField(new TextNode("key1"), new TextNode("value1"))
        ));

        when(scannableItem1.getOcrData()).thenReturn(ocrData);
        when(scannableItem1.getOcrValidationWarnings()).thenReturn(new String[]{"warning 1"});

        when(scannableItem2.getDocumentUuid()).thenReturn("documentUuid2");
        when(scannableItem2.getDocumentControlNumber()).thenReturn("doc2_control_number");
        when(scannableItem2.getFileName()).thenReturn("doc2_file_name");
        when(scannableItem2.getDocumentType()).thenReturn(DocumentType.FORM);
        when(scannableItem2.getDocumentSubtype()).thenReturn(DocumentSubtype.SSCS1);
        when(scannableItem2.getScanningDate()).thenReturn(Instant.now());
        when(scannableItem2.getOcrData()).thenReturn(null);

        when(payment1.getDocumentControlNumber()).thenReturn("dcn1");
        when(payment2.getDocumentControlNumber()).thenReturn("dcn2");
    }

    private void assertMessageContainsRightPayments(JsonNode jsonMessage) {
        JsonNode payments = jsonMessage.get("payments");
        assertThat(payments.isArray()).isTrue();
        assertThat(payments.size()).isEqualTo(2);

        assertThat(payments)
            .extracting(item -> item.get("document_control_number").asText())
            .containsExactly(
                payment1.getDocumentControlNumber(),
                payment2.getDocumentControlNumber()
            );
    }

    private void assertMessageContainsRightDocuments(JsonNode jsonMessage) {
        JsonNode documents = jsonMessage.get("documents");
        assertThat(documents).isNotNull();
        assertThat(documents.isArray()).isTrue();
        assertThat(documents.size()).isEqualTo(2);

        assertDocumentMatchesScannableItem(documents.get(0), scannableItem1);
        assertDocumentMatchesScannableItem(documents.get(1), scannableItem2);
    }

    @SuppressWarnings("unchecked")
    private void assertDocumentMatchesScannableItem(JsonNode jsonNode, ScannableItem scannableItem) {
        assertThat(jsonNode.get("file_name").asText()).isEqualTo(scannableItem.getFileName());
        assertThat(jsonNode.get("control_number").asText()).isEqualTo(scannableItem.getDocumentControlNumber());
        assertThat(jsonNode.get("type").asText()).isEqualTo(scannableItem.getDocumentType().toString());
        assertThat(jsonNode.get("uuid").asText()).isEqualTo(scannableItem.getDocumentUuid());
        assertDateField(jsonNode, "scanned_at", scannableItem.getScanningDate());
    }

    private void assertDateField(JsonNode jsonNode, String field, Instant expectedDate) {
        // instants serialised with 6 digit precision by default
        String iso8601DateTime = ZonedDateTime
            .ofInstant(expectedDate, ZoneId.of("UTC"))
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'"));

        assertThat(jsonNode.get(field).asText()).isEqualTo(iso8601DateTime);
    }
}
