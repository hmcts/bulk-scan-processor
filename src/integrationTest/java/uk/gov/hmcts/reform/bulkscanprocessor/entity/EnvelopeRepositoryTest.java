package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.persistence.EntityManager;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.EnvelopeCreator.envelope;

@SuppressWarnings("checkstyle:LineLength")
@RunWith(SpringRunner.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
public class EnvelopeRepositoryTest {

    @Autowired
    private EnvelopeRepository repo;

    @Autowired
    private EntityManager entityManager;

    @After
    public void cleanUp() {
        repo.deleteAll();
    }

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

    @Test
    public void setEnvelopeStatus_should_set_the_right_status_in_given_envelope() {
        Envelope envelope = envelope("BULKSCAN", Status.CREATED);

        UUID envelopeId = repo.saveAndFlush(envelope).getId();

        assertEnvelopeHasStatus(envelopeId, Status.CREATED);

        repo.setEnvelopeStatus(envelopeId, Status.CONSUMED);
        assertEnvelopeHasStatus(envelopeId, Status.CONSUMED);

        repo.setEnvelopeStatus(envelopeId, Status.PROCESSED);
        assertEnvelopeHasStatus(envelopeId, Status.PROCESSED);
    }

    @Test
    public void setEnvelopeStatus_should_not_update_status_of_any_other_envelope() {
        UUID envelope1Id = repo.saveAndFlush(envelope("BULKSCAN", Status.CREATED)).getId();
        UUID envelope2Id = repo.saveAndFlush(envelope("BULKSCAN", Status.CREATED)).getId();

        repo.setEnvelopeStatus(envelope1Id, Status.NOTIFICATION_SENT);
        assertEnvelopeHasStatus(envelope1Id, Status.NOTIFICATION_SENT);

        assertEnvelopeHasStatus(envelope2Id, Status.CREATED);
    }

    @Test
    public void setEnvelopeStatus_should_return_result_indicating_whether_update_took_place() {
        UUID existingEnvelopeId = repo.saveAndFlush(envelope("BULKSCAN", Status.CREATED)).getId();
        UUID nonExistingEnvelopeId = UUID.randomUUID();

        assertThat(repo.setEnvelopeStatus(existingEnvelopeId, Status.CONSUMED)).isOne();
        assertThat(repo.setEnvelopeStatus(nonExistingEnvelopeId, Status.CONSUMED)).isZero();
    }

    @Test
    public void findByZipFileName_should_find_envelops_in_db() {
        // given
        dbHas(
            envelope("A.zip", "X", Status.PROCESSED),
            envelope("A.zip", "Y", Status.UPLOAD_FAILURE),
            envelope("B.zip", "Z", Status.UPLOAD_FAILURE)
        );

        // when
        List<Envelope> resultForA = repo.findByZipFileName("A.zip");
        List<Envelope> resultForX = repo.findByZipFileName("X.zip");

        // then
        assertThat(resultForA).hasSize(2);
        assertThat(resultForX).hasSize(0);
    }

    private Envelope envelopeWithFailureCount(int failCount, String jurisdiction) throws Exception {
        Envelope envelope = envelope(jurisdiction, Status.UPLOAD_FAILURE);
        envelope.setUploadFailureCount(failCount);
        return envelope;
    }

    private void dbHas(Envelope... envelopes) {
        repo.saveAll(asList(envelopes));
    }

    private void assertEnvelopeHasStatus(UUID envelopeId, Status status) {
        // make sure there's no outdated cache
        entityManager.clear();

        Optional<Envelope> envelope = repo.findById(envelopeId);
        assertThat(envelope).isPresent();
        assertThat(envelope.get().getStatus()).isEqualTo(status);
    }
}
