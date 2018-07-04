package uk.gov.hmcts.reform.bulkscanprocessor.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.junit.Test;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.InvalidTimestampFormatException;

import java.io.IOException;
import java.sql.Timestamp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FailingTimestampDeserialiserTest {

    private static final StdDeserializer<Timestamp> DESERIALIZER = CustomTimestampDeserialiser.INSTANCE;

    private static final JsonParser PARSER = mock(JsonParser.class);

    private static final DeserializationContext CONTEXT = mock(DeserializationContext.class);

    @Test
    public void should_fail_to_parse_when_there_is_no_dot_for_fraction() throws IOException {
        when(PARSER.getText()).thenReturn("23-12-2019 03:04:05");

        Throwable exception = catchThrowable(() -> DESERIALIZER.deserialize(PARSER, CONTEXT));

        assertThat(exception).isInstanceOf(InvalidTimestampFormatException.class);
    }

    @Test
    public void should_fail_to_parse_when_having_incorrect_format() throws IOException {
        when(PARSER.getText()).thenReturn("2019-12-23 03:04:05.098765");

        Throwable exception = catchThrowable(() -> DESERIALIZER.deserialize(PARSER, CONTEXT));

        assertThat(exception).isInstanceOf(InvalidTimestampFormatException.class);
    }
}
