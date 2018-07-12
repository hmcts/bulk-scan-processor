package uk.gov.hmcts.reform.bulkscanprocessor.util;

import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeState;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeStatus;

public final class EnvelopeStatusBuilder {

    private static final EnvelopeStatusBuilder INSTANCE = new EnvelopeStatusBuilder();

    private static EnvelopeState envelopeState;

    private EnvelopeStatusBuilder() {
        // utility class constructor
    }

    public static EnvelopeStatusBuilder newEnvelopeStatus(String containerName, String zipFileName) {
        envelopeState = new EnvelopeState(containerName, zipFileName);

        return INSTANCE;
    }

    public EnvelopeStatusBuilder withStatus(EnvelopeStatus status) {
        envelopeState.setStatus(status);

        return this;
    }

    public EnvelopeStatusBuilder withReason(String reason) {
        envelopeState.setReason(reason);

        return this;
    }

    public EnvelopeStatusBuilder withEnvelope(Envelope envelope) {
        envelopeState.setEnvelope(envelope);

        return this;
    }

    public EnvelopeState build() {
        return envelopeState;
    }
}
