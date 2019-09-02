package uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg;

public enum MsgLabel {

    HEARTBEAT,
    TEST;

    @Override
    public String toString() {
        return name().toLowerCase();
    }

}
