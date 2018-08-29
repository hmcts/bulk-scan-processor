package uk.gov.hmcts.reform.bulkscanprocessor.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.InvalidSchemaException;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Component
public class EnvelopeSchemaValidator {

    private static final Logger log = LoggerFactory.getLogger(EnvelopeSchemaValidator.class);

    private final JsonSchema jsonSchemaValidator;

    public EnvelopeSchemaValidator(JsonSchema jsonSchemaValidator) {
        this.jsonSchemaValidator = jsonSchemaValidator;
    }

    /**
     * Validate object against envelope schema.
     * Throws an {@code InvalidSchemaException} in case there are errors.
     *
     * @param jsonObject to validate against
     * @throws ProcessingException processing error during the validation
     */
    public void validate(JsonNode jsonObject) throws ProcessingException {
        ProcessingReport report = jsonSchemaValidator.validate(jsonObject, true);

        if (!report.isSuccess()) {
            List<String> failures = StreamSupport
                .stream(report.spliterator(), false)
                .map(ProcessingMessage::getMessage)
                .collect(Collectors.toList());
            failures.forEach(log::error);

            throw new InvalidSchemaException(
                "There was a failure validating metadata file. Errors: " + failures.size()
            );
        }
    }
}
