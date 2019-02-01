package uk.gov.hmcts.reform.bulkscanprocessor.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.InvalidDateFormatException;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import static java.time.ZoneOffset.UTC;

public class CustomTimestampDeserialiser extends StdDeserializer<Instant> {

    public static final StdDeserializer<Instant> INSTANCE = new CustomTimestampDeserialiser();

    private static final String DATETIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    private CustomTimestampDeserialiser() {
        super(Instant.class);
    }

    @Override
    public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

        String timestampString = p.getText();

        try {
            return LocalDateTime.parse(timestampString, DateTimeFormatter.ofPattern(DATETIME_PATTERN)).toInstant(UTC);
        } catch (DateTimeParseException exception) {
            throw new InvalidDateFormatException(DATETIME_PATTERN, exception);
        }
    }
}
