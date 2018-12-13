package uk.gov.hmcts.reform.bulkscanprocessor.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static java.time.ZoneOffset.UTC;

public class CustomTimestampSerialiser extends StdSerializer<Timestamp> {

    private CustomTimestampSerialiser() {
        super(Timestamp.class);
    }

    @Override
    public void serialize(Timestamp value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        Instant utcInstant =
            ZonedDateTime.ofInstant(
                value.toInstant(),
                ZoneId.systemDefault()
            ).withZoneSameInstant(
                ZoneId.from(UTC)
            ).toInstant();

        gen.writeString(DateFormatter.getSimpleDateTime(utcInstant));
    }
}
