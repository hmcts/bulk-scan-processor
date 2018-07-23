package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;

public class DuplicateEnvelopeException extends RuntimeException {

    private final Envelope envelope;

    public DuplicateEnvelopeException(Envelope envelope) {
        super("Envelope already exists from " + envelope.getContainer() + " and " + envelope.getZipFileName());

        this.envelope = envelope;
    }

    public Envelope getEnvelope() {
        return envelope;
    }
}
