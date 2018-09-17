package uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg;

public class EnvelopeMsg implements Msg {

    private final String envelopeId;

    private final byte[] envelope;

    private final boolean testOnly;


    public EnvelopeMsg(String envelopeId) {
        this(envelopeId, false);
    }

    public EnvelopeMsg(String envelopeId, boolean testOnly) {
        this.envelopeId = envelopeId;
        this.envelope = new byte[0];
        this.testOnly = testOnly;
    }

    @Override
    public String getMsgId() {
        return envelopeId;
    }

    @Override
    public byte[] getMsgBody() {
        return envelope;
    }

    @Override
    public boolean isTestOnly() {
        return testOnly;
    }

    @Override
    public String toString() {
        return "EnvelopeMsg{"
            + "envelopeId='" + envelopeId + '\''
            + "testOnly='" + testOnly + '\''
            + '}';
    }

}
