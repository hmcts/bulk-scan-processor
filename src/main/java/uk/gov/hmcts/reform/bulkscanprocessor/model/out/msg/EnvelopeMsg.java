package uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg;

public class EnvelopeMsg implements Msg {

    private final String envelopeId;

    private final byte[] envelope;


    public EnvelopeMsg(String envelopeId) {
        this.envelopeId = envelopeId;
        this.envelope = new byte[0];
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
    public String toString() {
        return "EnvelopeMsg{"
            + "envelopeId='" + envelopeId + '\''
            + '}';
    }

}
