package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;

import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorCode.ERR_METAFILE_INVALID;

/**
 * Thrown when the envelope schema is invalid.
 */
public class InvalidEnvelopeSchemaException extends EnvelopeRejectionException {

    /**
     * Creates a new instance of the exception.
     *
     * @param report the validation report
     * @param zipFileName the name of the zip file
     */
    public InvalidEnvelopeSchemaException(ProcessingReport report, String zipFileName) {
        super(
            ERR_METAFILE_INVALID,
            String.format("Failed validation for file %s against schema. Errors:%n\t%s",
                zipFileName,
                StreamSupport
                    .stream(report.spliterator(), false)
                    .map(ProcessingMessage::toString)
                    .collect(Collectors.joining("\n\t"))
            )
        );
    }

    /**
     * Creates a new instance of the exception.
     * @param message the exception message
     * @param cause the cause of the exception
     */
    public InvalidEnvelopeSchemaException(String message, Throwable cause) {
        super(ERR_METAFILE_INVALID, message, cause);
    }
}
