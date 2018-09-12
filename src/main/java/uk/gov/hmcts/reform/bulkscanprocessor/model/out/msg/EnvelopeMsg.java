package uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg;

import com.google.common.base.Strings;

import java.nio.charset.StandardCharsets;

public class EnvelopeMsg implements Msg {

    private final String envelopeId;

    private final byte[] envelope;


    public EnvelopeMsg(String envelopeId) {
        this.envelopeId = envelopeId;
        this.envelope = new byte[0];
    }

    public EnvelopeMsg(String envelopeId, byte[] envelope) {
        this.envelopeId = envelopeId;
        this.envelope = (envelope != null ? envelope : new byte[0]);
    }

    public EnvelopeMsg(String envelopeId, String envelope) {
        this.envelopeId = envelopeId;
        this.envelope =
            (Strings.isNullOrEmpty(envelope) ? new byte[0] : envelope.getBytes(StandardCharsets.UTF_8));
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
