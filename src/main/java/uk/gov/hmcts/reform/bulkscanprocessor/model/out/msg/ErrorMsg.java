package uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg;

/**
 * Message describing an error processing an envelope.
 */
public class ErrorMsg implements Msg {

    public final String id;
    public final String zipFileName;
    public final String poBox;
    public final String documentControlNumber;
    public final ErrorCode errorCode;
    public final boolean isTestOnly;

    // region constructors
    public ErrorMsg(
        String id,
        String zipFileName,
        String poBox,
        String documentControlNumber,
        ErrorCode errorCode,
        boolean isTestOnly
    ) {
        this.id = id;
        this.zipFileName = zipFileName;
        this.poBox = poBox;
        this.documentControlNumber = documentControlNumber;
        this.errorCode = errorCode;
        this.isTestOnly = isTestOnly;
    }
    // endregion

    // region getters
    @Override
    public String getMsgId() {
        return this.id;
    }

    @Override
    public boolean isTestOnly() {
        return this.isTestOnly;
    }
    // endregion
}
