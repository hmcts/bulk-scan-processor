package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Instant;
import java.util.List;
import javax.persistence.EntityManager;

import static java.time.LocalDateTime.now;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.EnvelopeCreator.envelope;

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
    public void findEnvelopesToResend_should_not_return_envelopes_that_failed_to_be_sent_too_many_times() {
        // given
        final int maxFailCount = 5;

        dbHas(
            envelopeWithFailureCount(maxFailCount - 2),
            envelopeWithFailureCount(maxFailCount - 1),
            envelopeWithFailureCount(maxFailCount),
            envelopeWithFailureCount(maxFailCount + 1),
            envelopeWithFailureCount(maxFailCount + 2)
        );

        // when
        List<Envelope> result = repo.findEnvelopesToResend(maxFailCount);

        // then
        assertThat(result).hasSize(2);
    }

    @Test
    public void findEnvelopesToResend_should_filter_based_on_status() {
        // given
        dbHas(
            envelope("A", Status.CREATED),
            envelope("A", Status.UPLOAD_FAILURE),
            envelope("B", Status.UPLOAD_FAILURE)
        );

        // when
        List<Envelope> result = repo.findEnvelopesToResend(1_000);

        // then
        assertThat(result)
            .hasSize(2)
            .extracting(envelope -> tuple(envelope.getContainer(), envelope.getJurisdiction()))
            .containsOnly(
                tuple("SSCS", "A"),
                tuple("SSCS", "B")
            );
    }

    @Test
    public void findByZipFileName_should_find_envelops_in_db() {
        // given
        dbHas(
            envelope("A.zip", "X", Status.CREATED),
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

    @Test
    public void should_get_0_when_no_incomplete_envelopes_are_there_from_yesterday_backwards() {
        assertThat(repo.getIncompleteEnvelopesCountBefore(now().toLocalDate())).isEqualTo(0);
    }

    @Test
    public void should_count_envelopes_created_after_given_timestamp() {
        // given
        dbHas(
            envelope("A.zip", Status.COMPLETED),
            envelope("B.zip", Status.CONSUMED)
        );

        // then
        assertThat(repo.countAllByCreatedAtAfter(Instant.now().minusSeconds(1))).isEqualTo(2);
        assertThat(repo.countAllByCreatedAtAfter(Instant.now().plusSeconds(5))).isEqualTo(0);
    }

    @Test
    public void should_get_1_incomplete_envelope_from_yesterday() {
        // given
        dbHas(
            envelope("A.zip", "X", Status.UPLOADED),
            envelope("B.zip", "Y", Status.COMPLETED),
            envelope("C.zip", "Z", Status.UPLOAD_FAILURE),
            envelope("D.zip", "Z", Status.COMPLETED)
        );

        // and update createAt to yesterday
        entityManager.createNativeQuery(
            "UPDATE envelopes SET createdat = '" + now().minusDays(1) + "' WHERE zipfilename IN ('A.zip', 'B.zip')"
        ).executeUpdate();

        // then
        assertThat(repo.getIncompleteEnvelopesCountBefore(now().toLocalDate())).isEqualTo(1);
    }

    @Test
    public void should_find_complete_envelopes_by_container() {
        // given
        final String container1 = "container1";
        dbHas(
            envelope("X", Status.COMPLETED, container1),
            envelope("Y", Status.UPLOADED, "container2"),
            envelope("X", Status.COMPLETED, container1),
            envelope("X", Status.UPLOADED, container1),
            envelope("Z", Status.COMPLETED, "container3")
        );

        // when
        final List<Envelope> result = repo.findByContainerAndStatusAndZipDeleted(container1, Status.COMPLETED, false);

        // then
        assertThat(result)
            .hasSize(2)
            .extracting(envelope -> tuple(envelope.getStatus(), envelope.getContainer()))
            .containsExactlyInAnyOrder(
                tuple(Status.COMPLETED, container1),
                tuple(Status.COMPLETED, container1)
            );
    }

    @Test
    public void should_find_complete_envelopes_by_container_excluding_already_deleted() {
        // given
        final String container1 = "container1";
        dbHas(
            envelope("X", Status.COMPLETED, container1, false),
            envelope("Y", Status.UPLOADED, "container2", false),
            envelope("X", Status.COMPLETED, container1, true),
            envelope("X", Status.UPLOADED, container1, false),
            envelope("Z", Status.COMPLETED, "container3", false)
        );

        // when
        final List<Envelope> result = repo.findByContainerAndStatusAndZipDeleted(container1, Status.COMPLETED, false);

        // then
        assertThat(result)
            .hasSize(1)
            .extracting(envelope -> tuple(envelope.getStatus(), envelope.getContainer(), envelope.isZipDeleted()))
            .containsExactlyInAnyOrder(
                tuple(Status.COMPLETED, container1, false)
            );
    }

    @Test
    public void should_not_find_complete_envelopes_by_container_if_they_do_not_exist() {
        // given
        final String container1 = "container1";
        dbHas(
            envelope("X", Status.CREATED, container1),
            envelope("Y", Status.UPLOADED, "container2"),
            envelope("X", Status.CREATED, container1),
            envelope("X", Status.UPLOADED, container1),
            envelope("Z", Status.COMPLETED, "container3")
        );

        // when
        final List<Envelope> result = repo.findByContainerAndStatusAndZipDeleted(container1, Status.COMPLETED, false);

        // then
        assertThat(result).isEmpty();
    }

    private Envelope envelopeWithFailureCount(int failCount) {
        Envelope envelope = envelope("X", Status.UPLOAD_FAILURE);
        envelope.setUploadFailureCount(failCount);
        return envelope;
    }

    private void dbHas(Envelope... envelopes) {
        repo.saveAll(asList(envelopes));
    }
}
