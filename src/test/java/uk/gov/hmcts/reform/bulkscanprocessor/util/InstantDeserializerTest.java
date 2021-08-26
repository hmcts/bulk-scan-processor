package uk.gov.hmcts.reform.bulkscanprocessor.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InstantDeserializerTest {

    private static final StdDeserializer<Instant> DESERIALIZER = InstantDeserializer.INSTANCE;

    private static final JsonParser PARSER = mock(JsonParser.class);
    private static final DeserializationContext CONTEXT = mock(DeserializationContext.class);

    @Test
    void should_parse_json_date_field_as_instant() throws IOException {
        long milliseconds = 1530697192913L;
        Instant expected = Instant.ofEpochMilli(milliseconds);

        LocalDateTime localDateTime = ZonedDateTime.ofInstant(
            expected,
            ZoneId.systemDefault()
        ).withZoneSameInstant(
            ZoneId.from(UTC)
        ).toLocalDateTime();

        String date = String.format(
            "%d-%02d-%02dT%02d:%02d:%02d.%03dZ",
            localDateTime.getYear(),
            localDateTime.getMonthValue(),
            localDateTime.getDayOfMonth(),
            localDateTime.getHour(),
            localDateTime.getMinute(),
            localDateTime.getSecond(),
            milliseconds % 1000
        );

        when(PARSER.getText()).thenReturn(date);

        assertThat(DESERIALIZER.deserialize(PARSER, CONTEXT)).isEqualTo(expected);
    }
}
