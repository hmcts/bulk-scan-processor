package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

public class EntityParserTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void should_successfully_map_json_file_to_entities() throws IOException {
        InputStream inputStream = getClass().getResourceAsStream("/metafile.json");
        Envelope envelope = mapper.readValue(inputStream, Envelope.class);

        assertThat(envelope.getNonScannableItems()).hasSize(1);
        assertThat(envelope.getScannableItems()).hasSize(2);
        assertThat(envelope.getPayments()).hasSize(1);
        assertThat(envelope.getPayments().get(0).getAmount()).isEqualTo(100.0);
    }
}
