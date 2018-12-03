package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.helper.EnvelopeCreator;

import java.io.IOException;
import java.io.InputStream;
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
        Envelope envelope = EnvelopeCreator.envelope();

        // and
        UUID envelopeId = repository.save(envelope).getId();

        // when
        Envelope readEnvelope = repository.getOne(envelopeId);

        // then
        try (InputStream stream = EnvelopeCreator.getMetaFile()) {
            assertThat(readEnvelope).isEqualToComparingFieldByFieldRecursively(envelope);
        }
    }

    @Test
    public void should_log_a_warning_when_container_is_not_set() throws IOException {
        // given
        Envelope envelope = EnvelopeCreator.envelope();
        envelope.setContainer(null);

        // when
        Envelope dbEnvelope = repository.save(envelope);

        // then
        assertThat(capture.toString()).containsPattern(
            ".+ WARN  \\[.+\\] "
                + Envelope.class.getCanonicalName()
                + ":\\d+: Missing required container for .+\\.zip"
        );

        // and
        assertThat(dbEnvelope.getId()).isNotNull();
    }
}
