package uk.gov.hmcts.reform.bulkscanprocessor.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.InvalidDateFormatException;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeParseException;

public final class InstantDeserializer extends StdDeserializer<Instant> {

    public static final StdDeserializer<Instant> INSTANCE = new InstantDeserializer();

    private InstantDeserializer() {
        super(Instant.class);
    }

    @Override
    public Instant deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {

        String timestampString = parser.getText();

        try {
            return DateFormatter.getInstant(timestampString);
        } catch (DateTimeParseException exception) {
            throw new InvalidDateFormatException(DateFormatter.getPattern(), exception);
        }
    }
}
