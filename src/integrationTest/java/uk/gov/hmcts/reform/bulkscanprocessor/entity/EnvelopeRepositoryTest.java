package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import jakarta.persistence.EntityManager;

import static java.time.LocalDateTime.now;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.ABORTED;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.COMPLETED;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.CREATED;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.NOTIFICATION_SENT;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.UPLOADED;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.UPLOAD_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.EnvelopeCreator.envelope;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.EnvelopeCreator.scannableItems;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class EnvelopeRepositoryTest {

    @Autowired
    private EnvelopeRepository repo;

    @Autowired
    private EntityManager entityManager;

    @AfterEach
    public void cleanUp() {
        repo.deleteAll();
    }

    @Test
    public void findEnvelopesToUpload_should_not_return_envelopes_that_failed_to_be_uploaded_too_many_times() {
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
        List<Envelope> result = repo.findEnvelopesToUpload(maxFailCount);

        // then
        assertThat(result).hasSize(2);
    }

    @Test
    public void findEnvelopesToUpload_should_filter_based_on_status() {
        // given
        Envelope e1 = envelope("A", Status.CREATED);
        Envelope e2 = envelope("A", UPLOAD_FAILURE);
        Envelope e3 = envelope("B", UPLOADED);
        Envelope e4 = envelope("B", NOTIFICATION_SENT);

        dbHas(e1, e2, e3, e4);

        // when
        List<Envelope> result = repo.findEnvelopesToUpload(1_000);

        // then
        assertThat(result)
            .extracting(Envelope::getZipFileName)
            .containsExactlyInAnyOrder(
                e1.getZipFileName(),
                e2.getZipFileName()
            );
    }

    @Test
    public void findByZipFileName_should_find_envelops_in_db() {
        // given
        dbHas(
            envelope("A.zip", "X", Status.CREATED),
            envelope("A.zip", "Y", UPLOAD_FAILURE),
            envelope("B.zip", "Z", UPLOAD_FAILURE)
        );

        // when
        List<Envelope> resultForA = repo.findByZipFileName("A.zip");
        List<Envelope> resultForX = repo.findByZipFileName("X.zip");

        // then
        assertThat(resultForA).hasSize(2);
        assertThat(resultForX).hasSize(0);
    }

    @Test
    public void should_find_envelope_when_ccdid_matches() {
        // given
        Envelope e1 = envelope("A.zip", "X", Status.CREATED);
        e1.setCcdId("97586937");
        dbHas(e1);

        // when
        List<Envelope> result = repo.findByCcdId("97586937");
        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCcdId()).isEqualTo("97586937");
        assertThat(result.get(0).getZipFileName()).isEqualTo("A.zip");
        assertThat(result.get(0).getJurisdiction()).isEqualTo("X");
        assertThat(result.get(0).getStatus()).isEqualTo(CREATED);
    }

    @Test
    public void should_find_envelopes_when_ccdid_matches() {
        // given
        Envelope e1 = envelope("A.zip", "X", Status.CREATED);
        Envelope e2 = envelope("A.zip", "Y", UPLOAD_FAILURE);
        e1.setCcdId("1111123");
        e2.setCcdId("1111123");
        dbHas(e1, e2);
        // when
        List<Envelope> result = repo.findByCcdId("1111123");
        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCcdId()).isEqualTo("1111123");
        assertThat(result.get(1).getCcdId()).isEqualTo("1111123");
        assertThat(result.get(0).getZipFileName()).isEqualTo("A.zip");
        assertThat(result.get(1).getZipFileName()).isEqualTo("A.zip");
        assertThat(result.get(0).getJurisdiction()).isEqualTo("X");
        assertThat(result.get(1).getJurisdiction()).isEqualTo("Y");
        assertThat(result.get(0).getStatus()).isEqualTo(CREATED);
        assertThat(result.get(1).getStatus()).isEqualTo(UPLOAD_FAILURE);
    }

    @Test
    public void should_not_find_envelopes_when_ccdid_does_not_match() {
        // given
        Envelope e1 = envelope("A.zip", "X", Status.CREATED);
        Envelope e2 = envelope("A.zip", "Y", UPLOAD_FAILURE);
        Envelope e3 = envelope("B.zip", "Z", UPLOAD_FAILURE);
        e1.setCcdId("1111123");
        e2.setCcdId("1111123");
        e3.setCcdId("3546684");
        dbHas(e1, e2, e3);
        // when
        List<Envelope> result = repo.findByCcdId("786473");
        // then
        assertThat(result).hasSize(0);
    }

    @Test
    public void should_get_0_when_no_incomplete_envelopes_are_there_in_db() {
        assertThat(repo.getIncompleteEnvelopesCountBefore(now())).isEqualTo(0);
    }

    @Test
    public void should_count_envelopes_created_after_given_timestamp() {
        // given
        dbHas(
            envelope("A.zip", COMPLETED),
            envelope("B.zip", NOTIFICATION_SENT)
        );

        // then
        assertThat(repo.countAllByCreatedAtAfter(Instant.now().minusSeconds(1))).isEqualTo(2);
        assertThat(repo.countAllByCreatedAtAfter(Instant.now().plusSeconds(5))).isEqualTo(0);
    }

    @Test
    public void should_find_incomplete_envelopes() {
        // given
        dbHas(
            envelope("A.zip", "X", UPLOADED),
            envelope("B.zip", "Y", COMPLETED),
            envelope("C.zip", "Z", UPLOAD_FAILURE),
            envelope("D.zip", "Z", COMPLETED)
        );

        // and update createAt to 2h ago
        entityManager.createNativeQuery(
            "UPDATE envelopes SET createdat = '" + now().minusHours(2) + "' WHERE zipfilename IN ('A.zip', 'B.zip')"
        ).executeUpdate();

        // when
        int incompleteCount = repo.getIncompleteEnvelopesCountBefore(now().minusHours(1));

        // then
        assertThat(incompleteCount).isEqualTo(1);
    }

    @Test
    public void should_find_complete_envelopes_by_container() {
        // given
        final String container1 = "container1";
        dbHas(
            envelope("X", COMPLETED, container1),
            envelope("Y", UPLOADED, "container2"),
            envelope("X", COMPLETED, container1),
            envelope("X", UPLOADED, container1),
            envelope("Z", COMPLETED, "container3")
        );

        // when
        final List<Envelope> result = repo.findByContainerAndStatusAndZipDeleted(container1, COMPLETED, false);

        // then
        assertThat(result)
            .hasSize(2)
            .extracting(envelope -> tuple(envelope.getStatus(), envelope.getContainer()))
            .containsExactlyInAnyOrder(
                tuple(COMPLETED, container1),
                tuple(COMPLETED, container1)
            );
    }

    @Test
    public void should_find_complete_envelopes_by_container_excluding_already_deleted() {
        // given
        final String container1 = "container1";
        dbHas(
            envelope("X", COMPLETED, container1, false),
            envelope("Y", UPLOADED, "container2", false),
            envelope("X", COMPLETED, container1, true),
            envelope("X", UPLOADED, container1, false),
            envelope("Z", COMPLETED, "container3", false)
        );

        // when
        final List<Envelope> result = repo.findByContainerAndStatusAndZipDeleted(container1, COMPLETED, false);

        // then
        assertThat(result)
            .hasSize(1)
            .extracting(envelope -> tuple(envelope.getStatus(), envelope.getContainer(), envelope.isZipDeleted()))
            .containsExactlyInAnyOrder(
                tuple(COMPLETED, container1, false)
            );
    }

    @Test
    public void should_not_find_complete_envelopes_by_container_if_they_do_not_exist() {
        // given
        final String container1 = "container1";
        dbHas(
            envelope("X", Status.CREATED, container1),
            envelope("Y", UPLOADED, "container2"),
            envelope("X", Status.CREATED, container1),
            envelope("X", UPLOADED, container1),
            envelope("Z", COMPLETED, "container3")
        );

        // when
        final List<Envelope> result = repo.findByContainerAndStatusAndZipDeleted(container1, COMPLETED, false);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    public void should_get_empty_result_when_no_incomplete_envelopes_are_there_in_db() {
        assertThat(repo.getIncompleteEnvelopesBefore(now())).isEmpty();
    }

    @Test
    public void should_get_incomplete_envelopes() {
        // given
        dbHas(
            envelope("A.zip", "X", UPLOADED),
            envelope("B.zip", "Y", COMPLETED),
            envelope("C.zip", "Z", UPLOAD_FAILURE),
            envelope("D.zip", "Z", NOTIFICATION_SENT),
            envelope("E.zip", "Z", COMPLETED),
            envelope("F.zip", "Z", ABORTED)
        );

        // and update createAt to 2h ago
        entityManager.createNativeQuery(
            "UPDATE envelopes "
                + "SET createdat = '" + now().minusHours(2) + "' "
                + "WHERE zipfilename IN ('A.zip', 'B.zip', 'D.zip')"
        ).executeUpdate();

        // when
        List<Envelope> result = repo.getIncompleteEnvelopesBefore(now().minusHours(1));

        // then
        assertThat(result)
            .extracting(Envelope::getZipFileName)
            .containsExactlyInAnyOrder("A.zip", "D.zip");
    }

    @Test
    void should_get_complete_and_notified_envelopes() {
        // given
        dbHas(
            envelope("A.zip", "X", UPLOADED, scannableItems(), "c1", false),
            envelope("B.zip", "Y", COMPLETED, scannableItems(), "c1", false),
            envelope("C.zip", "Z", UPLOAD_FAILURE, scannableItems(), "c1", false),
            envelope("D.zip", "X", NOTIFICATION_SENT, scannableItems(), "c1", false),
            envelope("E.zip", "Y", COMPLETED, scannableItems(), "c2", false),
            envelope("F.zip", "Z", NOTIFICATION_SENT, scannableItems(), "c2", false),
            envelope("G.zip", "X", COMPLETED, scannableItems(), "c1", true),
            envelope("H.zip", "X", COMPLETED, scannableItems(), "c1", false)
        );

        // when
        List<Envelope> result = repo.getCompleteEnvelopesFromContainer("c1");

        // then
        assertThat(result)
            .extracting(Envelope::getZipFileName)
            .containsExactlyInAnyOrder("B.zip", "D.zip", "H.zip");
    }

    @Test
    public void findFirstByZipFileNameAndContainerOrderByCreatedAtDesc_should_return_one_envelope()
        throws InterruptedException {
        // given
        var envelope =
            envelope("A.zip", "X", UPLOADED, scannableItems(), "c1", false);
        TimeUnit.SECONDS.sleep(1);
        dbHas(
            envelope,
            envelope("A.zip", "X", COMPLETED, scannableItems(), "c1", false),
            envelope("B.zip", "Y", COMPLETED, scannableItems(), "c1", false),
            envelope("F.zip", "Z", NOTIFICATION_SENT, scannableItems(), "c2", false),
            envelope("A.zip", "X", COMPLETED, scannableItems(), "c2", false)
        );


        // when
        Envelope result = repo.findFirstByZipFileNameAndContainerOrderByCreatedAtDesc(
            "A.zip",
            "c1"
        );

        // then
        assertThat(result)
            .hasFieldOrPropertyWithValue("zipFileName", "A.zip")
            .hasFieldOrPropertyWithValue("container", "c1")
            .hasFieldOrPropertyWithValue("status", COMPLETED);
    }

    @Test
    public void findFirstByZipFileNameAndContainerOrderByCreatedAtDesc_should_return_null() {
        // when
        Envelope result = repo.findFirstByZipFileNameAndContainerOrderByCreatedAtDesc(
            "A.zip",
            "c1"
        );

        assertThat(result).isNull();

    }

    @Test
    public void findByJurisdictionAndCreatedAtGreaterThan_should_return_envelopes()
        throws InterruptedException {
        // given

        var e1 = envelope("A.zip", "X", COMPLETED, scannableItems(), "c1", false);
        Instant greaterThan = Instant.now();
        TimeUnit.SECONDS.sleep(2);
        var e2 = envelope("B.zip", "X", UPLOADED, scannableItems(), "c1", false);
        var e3 = envelope("F.zip", "X", NOTIFICATION_SENT, scannableItems(), "c2", false);
        var e4 = envelope("A.zip", "Y", COMPLETED, scannableItems(), "c2", false);

        dbHas(e1, e2, e3, e4);

        // when
        List<Envelope> results = repo.findByJurisdictionAndCreatedAtGreaterThan(
            "X",
            greaterThan
        );

        // then
        assertThat(results)
            .hasSize(2)
            .containsExactlyInAnyOrder(e2, e3);
    }

    @Test
    public void findByJurisdictionAndCreatedAtGreaterThan_should_not_return_any_envelope() {
        // given
        dbHas(
            envelope("A.zip", "X", COMPLETED, scannableItems(), "c1", false),
            envelope("B.zip", "X", COMPLETED, scannableItems(), "c1", false),
            envelope("F.zip", "X", NOTIFICATION_SENT, scannableItems(), "c2", false),
            envelope("A.zip", "X", COMPLETED, scannableItems(), "c2", false)
        );


        // when
        List<Envelope> results = repo.findByJurisdictionAndCreatedAtGreaterThan(
            "X",
            Instant.now()
        );

        // then
        assertThat(results).hasSize(0);
    }

    @Test
    public void findByJurisdictionAndStatusAndCreatedAtGreaterThan_should_return_envelopes()
        throws InterruptedException {
        // given

        var e1 = envelope("A.zip", "X", COMPLETED, scannableItems(), "c1", false);
        Instant greaterThan = Instant.now();
        TimeUnit.SECONDS.sleep(2);
        var e2 = envelope("B.zip", "X", COMPLETED, scannableItems(), "c1", false);
        var e3 = envelope("F.zip", "X", COMPLETED, scannableItems(), "c2", false);
        var e4 = envelope("A.zip", "Y", COMPLETED, scannableItems(), "c2", false);
        var e5 = envelope("A.zip", "X", UPLOADED, scannableItems(), "c2", false);

        dbHas(e1, e2, e3, e4, e5);

        // when
        List<Envelope> results = repo.findByJurisdictionAndStatusAndCreatedAtGreaterThan(
            "X",
            COMPLETED,
            greaterThan
        );

        // then
        assertThat(results)
            .hasSize(2)
            .containsExactlyInAnyOrder(e2, e3);
    }

    @Test
    public void findByJurisdictionAndStatusAndCreatedAtGreaterThan_should_not_return_any_envelope() {
        // given
        dbHas(
            envelope("A.zip", "X", COMPLETED, scannableItems(), "c1", false),
            envelope("B.zip", "X", COMPLETED, scannableItems(), "c1", false),
            envelope("F.zip", "X", NOTIFICATION_SENT, scannableItems(), "c2", false),
            envelope("A.zip", "X", COMPLETED, scannableItems(), "c2", false)
        );


        // when
        List<Envelope> results = repo.findByJurisdictionAndStatusAndCreatedAtGreaterThan(
            "X",
            COMPLETED,
            Instant.now()
        );

        // then
        assertThat(results).hasSize(0);
    }

    @Test
    void updateEnvelopeClassificationAndStatus_should_change_classification_and_status() {
        // given
        Envelope e1 = envelope("X", NOTIFICATION_SENT, "c1", Classification.SUPPLEMENTARY_EVIDENCE);
        Envelope e2 = envelope("X", COMPLETED, "c2", Classification.NEW_APPLICATION);
        dbHas(e1, e2);

        // when
        int updateCount = repo.updateEnvelopeClassificationAndStatus(e1.getId(), e1.getContainer());

        // then
        assertThat(updateCount).isEqualTo(1);
        assertThat(repo.findAll())
            .extracting(e ->
                            tuple(e.getId(), e.getClassification(), e.getStatus())
            )
            .containsExactlyInAnyOrder(
                tuple(e1.getId(), Classification.EXCEPTION, UPLOADED),
                tuple(e2.getId(), Classification.NEW_APPLICATION, COMPLETED)
            );
    }

    @Test
    void updateEnvelopeClassificationAndStatus_should_not_change_values_for_different_envelope_id() {
        // given
        Envelope e1 = envelope("X", NOTIFICATION_SENT, "c1", Classification.SUPPLEMENTARY_EVIDENCE);
        dbHas(e1);

        // when
        int updateCount = repo.updateEnvelopeClassificationAndStatus(UUID.randomUUID(), e1.getContainer());

        // then
        assertThat(updateCount).isEqualTo(0);
    }

    @Test
    void updateEnvelopeClassificationAndStatus_should_not_change_values_for_different_container() {
        // given
        Envelope e1 = envelope("X", NOTIFICATION_SENT, "c1", Classification.SUPPLEMENTARY_EVIDENCE);
        dbHas(e1);

        // when
        int updateCount = repo.updateEnvelopeClassificationAndStatus(e1.getId(), "test");

        // then
        assertThat(updateCount).isEqualTo(0);
    }

    @Test
    void updateEnvelopeClassificationAndStatus_should_not_change_values_for_different_classification() {
        // given
        Envelope e1 = envelope("X", NOTIFICATION_SENT, "c1", Classification.NEW_APPLICATION);
        dbHas(e1);

        // when
        int updateCount = repo.updateEnvelopeClassificationAndStatus(e1.getId(), "test");

        // then
        assertThat(updateCount).isEqualTo(0);
    }

    @Test
    void updateEnvelopeClassificationAndStatus_should_not_change_values_for_different_status() {
        // given
        Envelope e1 = envelope("X", ABORTED, "c1", Classification.SUPPLEMENTARY_EVIDENCE);
        dbHas(e1);

        // when
        int updateCount = repo.updateEnvelopeClassificationAndStatus(e1.getId(), "test");

        // then
        assertThat(updateCount).isEqualTo(0);
    }

    private Envelope envelopeWithFailureCount(int failCount) {
        Envelope envelope = envelope("X", UPLOAD_FAILURE);
        envelope.setUploadFailureCount(failCount);
        return envelope;
    }

    private void dbHas(Envelope... envelopes) {
        repo.saveAll(asList(envelopes));
        repo.flush();
    }
}
