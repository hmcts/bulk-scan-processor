package uk.gov.hmcts.reform.bulkscanprocessor.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.InvalidTimestampFormatException;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.ParseException;

/**
 * Custom deserialiser for Timestamp class.
 * Jackson library incorrectly deserialises string with microseconds (example suffix format: HH:mm:ss.SSSSSS).
 * Microseconds are part of the agreed date format provided in json metafile.
 * Jackson deserialiser takes milliseconds as seconds and breaks the correct representation.
 * Examples:
 *
 * <table>
 *     <tr>
 *         <th>String</th>
 *         <th>Timestamp</th>
 *     </tr>
 *     <tr>
 *         <td>12:00:00.100000</td>
 *         <td>12:01:40.000000</td>
 *     </tr>
 *     <tr>
 *         <td>12:00:00.010000</td>
 *         <td>12:00:10.000000</td>
 *     </tr>
 *     <tr>
 *         <td>12:00:00.001000</td>
 *         <td>12:00:01.000000</td>
 *     </tr>
 *     <tr>
 *         <td>12:00:00.000100</td>
 *         <td>12:00:00.000100</td>
 *     </tr>
 * </table>
 */
public class CustomTimestampDeserialiser extends StdDeserializer<Timestamp> {

    public static final StdDeserializer<Timestamp> INSTANCE = new CustomTimestampDeserialiser();

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
                throw new InvalidTimestampFormatException(DateFormatter.getPattern(), exception);
            }
        }

        throw new InvalidTimestampFormatException(DateFormatter.getPattern());
    }
}
