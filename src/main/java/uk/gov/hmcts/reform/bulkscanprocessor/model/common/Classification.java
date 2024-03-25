package uk.gov.hmcts.reform.bulkscanprocessor.model.common;

/**
 * The classification of the envelope.
 */
public enum Classification {

    EXCEPTION("exception"),
    NEW_APPLICATION("new_application"),
    SUPPLEMENTARY_EVIDENCE("supplementary_evidence"),
    SUPPLEMENTARY_EVIDENCE_WITH_OCR("supplementary_evidence_with_ocr");

    private final String value;

    /**
     * <p>We accept mixed cases here as that does not change the value semantically
     * and this allows us to be permissive in what we accept.</p>
     *
     * <p>When a classification value is instead serialized to Json only lowercase
     * is used as the corresponding field in the external model is a String which
     * is populated using the toString() method in this class (see EnvelopeResponse).
     * This means that we are strict with what we send.</p>
     *
     * @param value the classification type
     */
    Classification(final String value) {
        this.value = value.toLowerCase();
    }

    /**
     * To string.
     * @return the value
     */
    @Override
    public String toString() {
        return value;
    }

}
