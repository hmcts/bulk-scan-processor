package uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg;

/**
 * Represents a message that is sent to the queue.
 */
public interface Msg {

    String getMsgId();

    String getLabel();

}
