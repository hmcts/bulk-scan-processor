package uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg;

/**
 * Represents a message that is sent to the queue.
 */
public enum MsgLabel {

    HEARTBEAT,
    TEST;

    /**
     * Returns the name of the enum in lower case.
     */
    @Override
    public String toString() {
        return name().toLowerCase();
    }

}
