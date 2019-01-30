package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;

import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class InvalidEnvelopeSchemaException extends InvalidEnvelopeException {

    public InvalidEnvelopeSchemaException(ProcessingReport report, String zipFileName) {
        super(
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
        super(message, cause);
    }
}
