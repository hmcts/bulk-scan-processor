package uk.gov.hmcts.reform.bulkscanprocessor.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.ParseException;

public class CustomTimestampDeserialiser extends StdDeserializer<Timestamp> {

    private static final Logger log = LoggerFactory.getLogger(CustomTimestampDeserialiser.class);

    private CustomTimestampDeserialiser() {
        super(Timestamp.class);
    }

    @Override
    public Timestamp deserialize(JsonParser p, DeserializationContext ctxt)
        throws IOException, JsonProcessingException {

        String[] timestampString = p.getText().trim().split("\\.");

        if (timestampString.length == 2) {
            try {
                Timestamp timestamp = DateFormatter.getTimestamp(timestampString[0]);
                timestamp.setNanos(Integer.valueOf(timestampString[1]) * 1000);

                return timestamp;
            } catch (ParseException exception) {
                log.error(exception.getMessage(), exception);
            }
        }

        return null;
    }
}
