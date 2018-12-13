package uk.gov.hmcts.reform.bulkscanprocessor.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.InvalidTimestampFormatException;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.time.format.DateTimeParseException;

public class CustomTimestampDeserialiser extends StdDeserializer<Timestamp> {

    public static final StdDeserializer<Timestamp> INSTANCE = new CustomTimestampDeserialiser();

    private CustomTimestampDeserialiser() {
        super(Timestamp.class);
    }

    @Override
    public Timestamp deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

        String timestampString = p.getText();

        try {
            return DateFormatter.getTimestamp(timestampString);
        } catch (ParseException | DateTimeParseException exception) {
            throw new InvalidTimestampFormatException(DateFormatter.getPattern(), exception);
        }
    }
}
