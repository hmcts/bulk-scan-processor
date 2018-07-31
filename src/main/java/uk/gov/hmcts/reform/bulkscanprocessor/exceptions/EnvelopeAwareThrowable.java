package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;

public interface EnvelopeAwareThrowable extends EventRelatedThrowable {

    Envelope getEnvelope();
}
