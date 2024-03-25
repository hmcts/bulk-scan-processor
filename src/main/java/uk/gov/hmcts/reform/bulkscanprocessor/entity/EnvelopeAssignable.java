package uk.gov.hmcts.reform.bulkscanprocessor.entity;

/**
 * Interface for entities that can have an envelope.
 */
interface EnvelopeAssignable {

    /**
     * Sets the envelope for the entity.
     */
    void setEnvelope(Envelope envelope);
}
