package uk.gov.hmcts.reform.bulkscanprocessor.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.sql.Timestamp;

public class CustomTimestampSerialiser extends StdSerializer<Timestamp> {

    private CustomTimestampSerialiser() {
        super(Timestamp.class);
    }

    @Override
    public void serialize(Timestamp value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        String timestamp = DateFormatter.getSimpleDateTime(value);
        int micros = value.getNanos() / 1000;

        gen.writeString(timestamp + "." + String.format("%06d", micros));
    }
}
