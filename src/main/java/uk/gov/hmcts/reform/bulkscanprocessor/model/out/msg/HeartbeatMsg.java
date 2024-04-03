package uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg;

import java.util.UUID;

/**
 * Represents a heartbeat message.
 */
public class HeartbeatMsg implements Msg {

    private final String id;

    /**
     * Constructor.
     */
    public HeartbeatMsg() {
        this.id = UUID.randomUUID().toString();
    }

    /**
     * Get the message ID.
     * @return The message ID
     */
    @Override
    public String getMsgId() {
        return this.id;
    }

    /**
     * Get the message label.
     * @return The message label
     */
    @Override
    public String getLabel() {
        return MsgLabel.HEARTBEAT.toString();
    }
}
