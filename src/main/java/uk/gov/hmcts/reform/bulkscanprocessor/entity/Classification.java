package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import com.google.common.base.Strings;

public enum Classification {

    EXCEPTION("exception"),
    NEW_APPLICATION("new_application"),
    SUPPLEMENTARY_EVIDENCE("supplementary_evidence");

    private final String value;

    Classification(final String value) {
        this.value = (Strings.isNullOrEmpty(value) ? null : value.toLowerCase());
    }

    @Override
    public String toString() {
        return value;
    }

}
