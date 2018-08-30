package uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg;

import java.util.UUID;

public abstract class Msg {

    private final String msgId;

    public Msg() {
        this.msgId = UUID.randomUUID().toString();
    }

    public Msg(String msgId) {
        this.msgId = msgId;
    }


    public String getMsgId() {
        return msgId;
    }

}
