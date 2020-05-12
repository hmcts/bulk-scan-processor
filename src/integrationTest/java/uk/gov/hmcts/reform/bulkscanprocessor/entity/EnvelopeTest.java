package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.helper.EnvelopeCreator;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
public class EnvelopeTest {

    @Autowired
    private EnvelopeRepository repository;

    @After
    public void cleanUp() {
        repository.deleteAll();
    }

    @Test
    public void should_insert_into_db_and_retrieve_the_same_envelope() throws IOException {
        // given
        Envelope envelope = EnvelopeCreator.envelope();

        // and
        UUID envelopeId = repository.saveAndFlush(envelope).getId();

        // when
        Envelope readEnvelope = repository.getOne(envelopeId);

        // then
        assertThat(readEnvelope).isEqualToComparingFieldByFieldRecursively(envelope);
    }

    @Test
    public void should_log_a_warning_when_container_is_not_set() throws IOException {
        // given
        Envelope envelope = EnvelopeCreator.envelope();
        envelope.setContainer(null);

        // when
        Envelope dbEnvelope = repository.saveAndFlush(envelope);

        // then
        assertThat(dbEnvelope.getId()).isNotNull();
    }
}
