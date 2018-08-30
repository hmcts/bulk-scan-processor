package uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg;

public class EnvelopeMsg extends Msg {

    private final String envelopeId;

    public EnvelopeMsg(String envelopeId) {
        super();
        this.envelopeId = envelopeId;
    }

}
