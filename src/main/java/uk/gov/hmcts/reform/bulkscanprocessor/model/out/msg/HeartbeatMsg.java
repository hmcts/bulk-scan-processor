package uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg;

import java.util.UUID;

public class HeartbeatMsg implements Msg {

    private final String id;

    public HeartbeatMsg() {
        this.id = UUID.randomUUID().toString();
    }

    @Override
    public String getMsgId() {
        return this.id;
    }

    @Override
    public boolean isTestOnly() {
        return false;
    }
}
