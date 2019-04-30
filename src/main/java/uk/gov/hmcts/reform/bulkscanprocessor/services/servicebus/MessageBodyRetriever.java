package uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus;

import com.microsoft.azure.servicebus.MessageBody;
import org.springframework.util.CollectionUtils;

import java.util.List;

public final class MessageBodyRetriever {

    public static byte[] getBinaryData(MessageBody messageBody) {
        List<byte[]> binaryData = messageBody.getBinaryData();

        return CollectionUtils.isEmpty(binaryData) ? null : binaryData.get(0);
    }

    private MessageBodyRetriever() {
        // utility class construct
    }
}
