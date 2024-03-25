package uk.gov.hmcts.reform.bulkscanprocessor.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.time.Instant;

/**
 * Serializer for {@link Instant} objects.
 */
public final class InstantSerializer extends StdSerializer<Instant> {

    /**
     * Creates an instance of this instant serializer.
     */
    private InstantSerializer() {
        super(Instant.class);
    }

    /**
     * Serializes an {@link Instant} object into a string.
     *
     * @param value the instant object
     * @param gen the generator
     * @param provider the provider
     * @throws IOException if there is an error writing the output
     */
    @Override
    public void serialize(Instant value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeString(DateFormatter.getSimpleDateTime(value));
    }
}
