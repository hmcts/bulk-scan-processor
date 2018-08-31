package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;

import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class InvalidEnvelopeSchemaException extends RuntimeException {

    public InvalidEnvelopeSchemaException(ProcessingReport report) {
        super("Failed validation against schema:\n\t" + StreamSupport
            .stream(report.spliterator(), false)
            .map(ProcessingMessage::getMessage)
            .collect(Collectors.joining("\n\t"))
        );
    }
}
