package uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg;

/**
 * Message describing an error processing an envelope.
 */
public class ErrorMsg implements Msg {

    public final String id;
    public final String zipFileName;
    public final String jurisdiction;
    public final String poBox;
    public final String documentControlNumber;
    public final ErrorCode errorCode;
    public final String errorDescription;
    public final boolean testOnly;

    // region constructors
    @SuppressWarnings("squid:S00107") // number of params
    public ErrorMsg(
        String id,
        String zipFileName,
        String jurisdiction,
        String poBox,
        String documentControlNumber,
        ErrorCode errorCode,
        String errorDescription,
        boolean testOnly
    ) {
        this.id = id;
        this.zipFileName = zipFileName;
        this.jurisdiction = jurisdiction;
        this.poBox = poBox;
        this.documentControlNumber = documentControlNumber;
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
        this.testOnly = testOnly;
    }
    // endregion

    // region getters
    @Override
    public String getMsgId() {
        return this.id;
    }

    @Override
    public boolean isTestOnly() {
        return this.testOnly;
    }
    // endregion
}
