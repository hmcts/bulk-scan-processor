package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.util.EntityParser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
public class EnvelopeTest {

    @Rule
    public OutputCapture capture = new OutputCapture();

    @Autowired
    private EnvelopeRepository repository;

    @After
    public void cleanUp() {
        repository.deleteAll();
    }

    @Test
    public void should_insert_into_db_and_validate_data_correctness_when_retrieved() throws IOException {
        // given
        Envelope envelope = EntityParser.parseEnvelopeMetadata(getMetaFile());
        envelope.setContainer("container");

        // and
        UUID envelopeId = repository.save(envelope).getId();

        // when
        Envelope dbEnvelope = repository.getOne(envelopeId);

        // then
        try (InputStream stream = getMetaFile()) {
            String originalMetaFile = IOUtils.toString(stream, Charset.defaultCharset());
            String actualEnvelope = new ObjectMapper().writeValueAsString(dbEnvelope);

            /*
            Properties to ignore because:
            - id - autogenerated by us
            - zip_file_created_date - because we fix wording incorrectly provided in metafile
            - amount / amount_in_pence - converting to integers/doubles and tested in unit case already
            - configuration/json - auto-included fields by JsonPath/assertj when merging inner objects
             */
            assertThat(JsonPath.parse(actualEnvelope)).isEqualToIgnoringGivenFields(
                JsonPath.parse(originalMetaFile),
                "id", "zip_file_created_date", "amount", "amount_in_pence", "configuration", "json"
            );
        }
    }

    @Test
    public void should_log_a_warning_when_container_is_not_set() throws IOException {
        // given
        Envelope envelope = EntityParser.parseEnvelopeMetadata(getMetaFile());

        // when
        Envelope dbEnvelope = repository.save(envelope);

        // then
        assertThat(capture.toString()).containsPattern(
            ".+ WARN  \\[.+\\] " + Envelope.class.getCanonicalName() + ":\\d+: Missing required container for .+\\.zip"
        );

        // and
        assertThat(dbEnvelope.getId()).isNotNull();
    }

    private InputStream getMetaFile() {
        return getClass().getResourceAsStream("/metafile.json");
    }
}
