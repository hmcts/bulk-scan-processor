package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;

import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorCode.ERR_METAFILE_INVALID;

public class InvalidEnvelopeSchemaException extends EnvelopeRejectionException {

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

    public InvalidEnvelopeSchemaException(String message, Throwable cause) {
        super(ERR_METAFILE_INVALID, message, cause);
    }
}
