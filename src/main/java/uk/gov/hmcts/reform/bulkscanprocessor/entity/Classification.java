package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import com.google.common.base.Strings;

public enum Classification {

    EXCEPTION("exception"),
    NEW_APPLICATION("new_application"),
    SUPPLEMENTARY_EVIDENCE("supplementary_evidence");

    private final String value;

    /**
     * We accept mixed cases here as that does not change the value semantically
     * and this allows us to be permissive in what we accept.
     *
     * When a classification value is instead serialized to Json only lowercase
     * is used as the corresponding field in the external model is a String which
     * is populated using the toString() method in this class (see EnvelopeResponse).
     * This means that we are strict with what we send.
     *
     * @param value
     */
    Classification(final String value) {
        this.value = (Strings.isNullOrEmpty(value) ? null : value.toLowerCase());
    }

    @Override
    public String toString() {
        return value;
    }

}
