package uk.gov.hmcts.reform.bulkscanprocessor.helper;

import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputEnvelope;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification;

import java.util.Collections;

import static java.util.Collections.emptyList;

public final class InputEnvelopeCreator {

    private InputEnvelopeCreator() {
        // util class
    }

    public static InputEnvelope forJurisdiction(String jurisdiction) {
        return new InputEnvelope(
            "poBox",
            jurisdiction,
            null,
            null,
            null,
            "file.zip",
            "case_number",
            Classification.EXCEPTION,
            emptyList(),
            emptyList(),
            emptyList()
        );
    }
}
