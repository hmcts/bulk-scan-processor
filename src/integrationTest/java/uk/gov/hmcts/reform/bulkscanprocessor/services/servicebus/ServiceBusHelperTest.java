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

import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ServiceBusHelperTest {

    @Mock
    private IQueueClient queueClient;

    @Mock
    private QueueClientSupplier queueClientSupplier;

    private ServiceBusHelper serviceBusHelper;

    @Autowired
    private ObjectMapper objectMapper;

    @Mock
    private Envelope envelope;

    private UUID envelopeId = UUID.randomUUID();

    @Mock
    private ScannableItem scannableItem1;

    @Mock
    private ScannableItem scannableItem2;

    @Before
    public void setUp() {
        when(queueClientSupplier.get()).thenReturn(this.queueClient);
        serviceBusHelper = new ServiceBusHelper(queueClientSupplier, this.objectMapper);

        mockEnevelopeData();
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
    public void should_not_add_any_label_to_standard_message() {
        Msg msg = new EnvelopeMsg(envelope);
        Message busMessage = serviceBusHelper.mapToBusMessage(msg);
        assertThat(busMessage.getLabel()).isNullOrEmpty();
    }

    @Test
    public void should_send_message_with_envelope_data() throws Exception {

        Msg message = new EnvelopeMsg(envelope);
        Message busMessage = serviceBusHelper.mapToBusMessage(message);

        JsonNode jsonNode = objectMapper.readTree(busMessage.getBody());

        assertThat(jsonNode.get("case_ref").textValue()).isEqualTo("1111222233334446");
        assertThat(jsonNode.get("jurisdiction").textValue()).isEqualTo("SSCS");
        assertThat(jsonNode.get("zip_file_name").textValue()).isEqualTo("zip-file-test.zip");
        assertThat(jsonNode.get("classification").textValue()).isEqualTo(Classification.EXCEPTION.name());

        JsonNode docUrls = jsonNode.get("doc_urls");
        assertThat(docUrls.isArray()).isTrue();
        assertThat(docUrls.size()).isEqualTo(2);
        assertThat(docUrls.get(0).asText()).isEqualTo("documentUrl1");
        assertThat(docUrls.get(1).asText()).isEqualTo("documentUrl2");
    }

    private void mockEnevelopeData() {
        when(envelope.getId()).thenReturn(envelopeId);
        when(envelope.getCaseNumber()).thenReturn("1111222233334446");
        when(envelope.getJurisdiction()).thenReturn("SSCS");
        when(envelope.getZipFileName()).thenReturn("zip-file-test.zip");
        when(envelope.getClassification()).thenReturn(Classification.EXCEPTION);
        when(envelope.getScannableItems()).thenReturn(Arrays.asList(scannableItem1, scannableItem2));
        when(scannableItem1.getDocumentUrl()).thenReturn("documentUrl1");
        when(scannableItem2.getDocumentUrl()).thenReturn("documentUrl2");
    }

}
