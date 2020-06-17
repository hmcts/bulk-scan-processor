package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.helper.EnvelopeCreator;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
@ExtendWith(SpringExtension.class)
public class EnvelopeTest {

    @Autowired
    private EnvelopeRepository repository;

    @AfterEach
    public void cleanUp() {
        repository.deleteAll();
    }

    @Test
    public void should_insert_into_db_and_retrieve_the_same_envelope() {
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
    public void should_log_a_warning_when_container_is_not_set() {
        // given
        Envelope envelope = EnvelopeCreator.envelope();
        envelope.setContainer(null);

        // when
        Envelope dbEnvelope = repository.saveAndFlush(envelope);

        // then
        assertThat(dbEnvelope.getId()).isNotNull();
    }
}
