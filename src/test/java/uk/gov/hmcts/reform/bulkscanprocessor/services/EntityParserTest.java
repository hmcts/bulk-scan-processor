package uk.gov.hmcts.reform.bulkscanprocessor.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

public class EntityParserTest {

    @Test
    public void should_successfully_map_json_file_to_entities() throws IOException {
        InputStream inputStream = getClass().getResourceAsStream("/metafile.json");
        EntityParser parser = new EntityParser(new ObjectMapper());
        Envelope envelope = parser.parseEnvelopeMetadata(inputStream);

        assertThat(envelope.getNonScannableItems()).hasSize(1);
        assertThat(envelope.getScannableItems()).hasSize(2);
        assertThat(envelope.getPayments()).hasSize(1);
        assertThat(envelope.getPayments().get(0).getAmount()).isEqualTo(100.0);
    }
}
