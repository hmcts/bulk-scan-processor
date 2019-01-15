package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.EnvelopeCountSummaryItem;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.EnvelopeCountSummaryRepository;

import java.time.LocalDate;
import java.util.List;

import static java.time.LocalDate.now;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.CONSUMED;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.CREATED;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.METADATA_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.NOTIFICATION_SENT;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.PROCESSED;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.SIGNATURE_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.UPLOADED;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.UPLOAD_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.EnvelopeCreator.envelope;

@RunWith(SpringRunner.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
public class EnvelopeCountSummaryRepositoryTest {

    @Autowired private EnvelopeCountSummaryRepository reportRepo;
    @Autowired private EnvelopeRepository envelopesRepo;

    @Test
    public void should_group_by_jurisdiction() {
        // given
        dbHas(
            envelope("A", PROCESSED),
            envelope("A", PROCESSED),
            envelope("A", PROCESSED),
            envelope("B", PROCESSED),
            envelope("B", PROCESSED),
            envelope("C", PROCESSED)
        );

        // when
        List<EnvelopeCountSummaryItem> result = reportRepo.getReportFor(now());

        // then
        assertThat(result)
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new Item(now(), "A", 3, 0),
                new Item(now(), "B", 2, 0),
                new Item(now(), "C", 1, 0)
            );
    }

    @Test
    public void should_count_rejected_envelopes_properly() {
        // given
        List<Envelope> envelopes =
            asList(
                envelope(CONSUMED.toString(), CONSUMED),
                envelope(CREATED.toString(), CREATED),
                envelope(METADATA_FAILURE.toString(), METADATA_FAILURE),
                envelope(SIGNATURE_FAILURE.toString(), SIGNATURE_FAILURE),
                envelope(PROCESSED.toString(), PROCESSED),
                envelope(UPLOADED.toString(), UPLOADED),
                envelope(UPLOAD_FAILURE.toString(), UPLOAD_FAILURE),
                envelope(NOTIFICATION_SENT.toString(), NOTIFICATION_SENT)
            );

        assertThat(envelopes)
            .extracting(Envelope::getStatus)
            .as("All envelope statuses should be checked in this test")
            .containsExactlyInAnyOrder(Status.values());

        envelopesRepo.saveAll(envelopes);

        // when
        List<EnvelopeCountSummaryItem> result = reportRepo.getReportFor(now());

        // then
        assertThat(result)
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new Item(now(), CONSUMED.toString(), 1, 0),
                new Item(now(), CREATED.toString(), 1, 0),
                new Item(now(), METADATA_FAILURE.toString(), 1, 1),
                new Item(now(), SIGNATURE_FAILURE.toString(), 1, 1),
                new Item(now(), PROCESSED.toString(), 1, 0),
                new Item(now(), UPLOADED.toString(), 1, 0),
                new Item(now(), UPLOAD_FAILURE.toString(), 1, 0),
                new Item(now(), NOTIFICATION_SENT.toString(), 1, 0)
            );
    }

    @Test
    public void should_filter_by_date() {
        // given
        dbHas(
            envelope("X", PROCESSED),
            envelope("Y", PROCESSED)
        );

        // when
        List<EnvelopeCountSummaryItem> resultForYesterday = reportRepo.getReportFor(now().minusDays(1));

        // then
        assertThat(resultForYesterday).isEmpty();
    }

    private void dbHas(Envelope... envelopes) {
        envelopesRepo.saveAll(asList(envelopes));
    }

    // region helper model
    class Item implements EnvelopeCountSummaryItem {
        private final LocalDate date;
        private final String jurisdiction;
        private final int received;
        private final int rejected;

        public Item(LocalDate date, String jurisdiction, int received, int rejected) {
            this.date = date;
            this.jurisdiction = jurisdiction;
            this.received = received;
            this.rejected = rejected;
        }

        @Override
        public LocalDate getDate() {
            return date;
        }

        @Override
        public String getJurisdiction() {
            return jurisdiction;
        }

        @Override
        public int getReceived() {
            return received;
        }

        @Override
        public int getRejected() {
            return rejected;
        }
    }
    // endregion
}
