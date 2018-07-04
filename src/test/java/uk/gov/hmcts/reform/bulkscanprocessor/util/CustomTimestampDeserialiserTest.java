package uk.gov.hmcts.reform.bulkscanprocessor.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class CustomTimestampDeserialiserTest {

    private static final StdDeserializer<Timestamp> DESERIALIZER = CustomTimestampDeserialiser.INSTANCE;

    private final int micros;
    private static final JsonParser PARSER = mock(JsonParser.class);
    private static final DeserializationContext CONTEXT = mock(DeserializationContext.class);

    @Parameterized.Parameters(name = "Check deserialisation with {0} microseconds")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {
            { 100000 },
            { 10000 },
            { 1000 },
            { 100 },
            { 10 },
            { 1 }
        });
    }

    public CustomTimestampDeserialiserTest(int micros) {
        this.micros = micros;
    }

    @Test
    public void should_parse_json_field_as_timestamp() throws IOException {
        Timestamp expected = new Timestamp(1530697192913L);
        expected.setNanos(micros * 1000);
        LocalDateTime localDateTime = expected.toLocalDateTime();
        String date = String.format(
            "%02d-%02d-%d %02d:%02d:%02d.%06d",
            localDateTime.getDayOfMonth(),
            localDateTime.getMonthValue(),
            localDateTime.getYear(),
            localDateTime.getHour(),
            localDateTime.getMinute(),
            localDateTime.getSecond(),
            micros
        );

        when(PARSER.getText()).thenReturn(date);

        assertThat(DESERIALIZER.deserialize(PARSER, CONTEXT)).hasSameTimeAs(expected);
    }
}
