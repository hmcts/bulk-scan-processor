package uk.gov.hmcts.reform.bulkscanprocessor.util;

import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeStatus;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeStatusEnum;

public final class EnvelopeStatusBuilder {

    private static final EnvelopeStatusBuilder INSTANCE = new EnvelopeStatusBuilder();

    private static EnvelopeStatus envelopeStatus;

    private EnvelopeStatusBuilder() {
        // utility class constructor
    }

    public static EnvelopeStatusBuilder newEnvelopeStatus(String container, String zipFileName) {
        envelopeStatus = new EnvelopeStatus(container, zipFileName);

        return INSTANCE;
    }

    public EnvelopeStatusBuilder withStatus(EnvelopeStatusEnum status) {
        envelopeStatus.setStatus(status);

        return this;
    }

    public EnvelopeStatusBuilder withReason(String reason) {
        envelopeStatus.setReason(reason);

        return this;
    }

    public EnvelopeStatusBuilder withEnvelope(Envelope envelope) {
        envelopeStatus.setEnvelope(envelope);

        return this;
    }

    public EnvelopeStatus build() {
        return envelopeStatus;
    }
}
