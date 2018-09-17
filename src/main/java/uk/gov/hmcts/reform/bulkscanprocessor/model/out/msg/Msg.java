package uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg;


public interface Msg {

    public static final String TEST_MSG_LABEL = "test";

    String getMsgId();

    byte[] getMsgBody();

    boolean isTestOnly();
    
}
