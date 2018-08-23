package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.EnvelopeCreator.envelope;

@SuppressWarnings("checkstyle:LineLength")
@RunWith(SpringRunner.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
public class EnvelopeRepoTest {

    @Autowired
    private EnvelopeRepository repo;

    @Test
    public void findEnvelopesToResend_should_not_return_envelopes_that_failed_to_be_sent_too_many_times() throws Exception {
        // given
        final String jurisdiction = "X";
        final int maxFailCount = 5;

        dbHas(
            envelopeWithFailureCount(maxFailCount - 2, jurisdiction),
            envelopeWithFailureCount(maxFailCount - 1, jurisdiction),
            envelopeWithFailureCount(maxFailCount, jurisdiction),
            envelopeWithFailureCount(maxFailCount + 1, jurisdiction),
            envelopeWithFailureCount(maxFailCount + 2, jurisdiction)
        );

        // when
        List<Envelope> result = repo.findEnvelopesToResend(jurisdiction, maxFailCount, null);

        // then
        assertThat(result).hasSize(2);
    }

    @Test
    public void findEnvelopesToResend_should_filter_based_on_status_and_jurisdiction() throws Exception {
        // given
        dbHas(
            envelope("A", Status.PROCESSED),
            envelope("A", Status.UPLOAD_FAILURE),
            envelope("B", Status.UPLOAD_FAILURE)
        );

        // when
        List<Envelope> result = repo.findEnvelopesToResend("A", 1_000, null);

        // then
        assertThat(result).hasSize(1);
    }

    @After
    public void cleanUp() {
        repo.deleteAll();
    }

    private Envelope envelopeWithFailureCount(int failCount, String jurisdiction) throws Exception {
        Envelope envelope = envelope(jurisdiction, Status.UPLOAD_FAILURE);
        envelope.setUploadFailureCount(failCount);
        return envelope;
    }

    private void dbHas(Envelope... envelopes) {
        Arrays.asList(envelopes).forEach(env -> repo.save(env));
    }
}
