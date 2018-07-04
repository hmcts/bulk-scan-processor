package uk.gov.hmcts.reform.bulkscanprocessor.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.sql.Timestamp;

/**
 * Custom serialiser for Timestamp class.
 * Jackson library incorrectly serialises object with microseconds (example suffix format: HH:mm:ss.SSSSSS).
 * Microseconds come back from postgres database (timestamp data type only saves up to microseconds).
 * Timestamp object assigns as nanoseconds correctly.
 * Jackson serialiser takes milliseconds as seconds and breaks the correct representation.
 * Examples:
 *
 * <table>
 *     <tr>
 *         <th>Timestamp</th>
 *         <th>String</th>
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
