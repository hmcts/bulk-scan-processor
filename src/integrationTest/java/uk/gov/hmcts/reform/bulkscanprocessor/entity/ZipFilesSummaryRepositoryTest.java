package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.ZipFileSummaryItem;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.ZipFilesSummaryRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.helper.EnvelopeCreator;
import uk.gov.hmcts.reform.bulkscanprocessor.helper.reports.zipfilesummary.Item;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.COMPLETED;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.CONSUMED;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.PROCESSED;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.DOC_PROCESSED_NOTIFICATION_SENT;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.FILE_VALIDATION_FAILURE;

@RunWith(SpringRunner.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
public class ZipFilesSummaryRepositoryTest {

    @Autowired
    private ZipFilesSummaryRepository reportRepo;
    @Autowired
    private ProcessEventRepository eventRepo;
    @Autowired
    private EnvelopeRepository envelopeRepo;

    @Test
    public void should_join_envelopes_and_events_and_return_summary_by_date() {
        // given
        Envelope envelope1 = envelope("c1", EnvelopeCreator.envelope("test1.zip", "BULKSCAN", COMPLETED));
        Envelope envelope2 = envelope("c2", EnvelopeCreator.envelope("test2.zip", "BULKSCAN", CONSUMED));
        Envelope envelope3 = envelope("c3", EnvelopeCreator.envelope("test3.zip", "BULKSCAN", PROCESSED));

        dbHasEnvelopes(envelope1, envelope2, envelope3);

        Instant completedAt = envelope1.getCreatedAt().plus(2, HOURS);

        dbHasEvents(
            event("c1", "test1.zip", envelope1.getCreatedAt().plus(1, HOURS), DOC_PROCESSED_NOTIFICATION_SENT),
            event("c1", "test1.zip", completedAt, Event.COMPLETED),
            event("c2", "test2.zip", envelope2.getCreatedAt().plus(5, HOURS), DOC_PROCESSED_NOTIFICATION_SENT),
            event("c4", "test4.zip", envelope2.getCreatedAt().plus(3, HOURS), FILE_VALIDATION_FAILURE)
        );

        // when
        List<ZipFileSummaryItem> result = reportRepo.getZipFileSummaryReportFor(LocalDate.now());

        // then
        assertThat(result)
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new Item("test1.zip", envelope1.getCreatedAt(), completedAt, "BULKSCAN", COMPLETED.toString()),
                new Item("test1.zip", envelope1.getCreatedAt(), completedAt, "BULKSCAN", COMPLETED.toString()),
                new Item("test2.zip", envelope2.getCreatedAt(), null, "BULKSCAN", CONSUMED.toString()),
                new Item("test3.zip", envelope3.getCreatedAt(), null, "BULKSCAN", PROCESSED.toString())
            );
    }

    @Test
    public void should_return_all_envelopes_when_no_events_are_registered() {
        // given
        Envelope envelope1 = envelope("c1", EnvelopeCreator.envelope("test1.zip", "BULKSCAN", CONSUMED));
        Envelope envelope2 = envelope("c2", EnvelopeCreator.envelope("test2.zip", "BULKSCAN", CONSUMED));

        dbHasEnvelopes(envelope1, envelope2);

        // when
        List<ZipFileSummaryItem> result = reportRepo.getZipFileSummaryReportFor(LocalDate.now());

        // then
        assertThat(result)
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new Item("test1.zip", envelope1.getCreatedAt(), null, "BULKSCAN", CONSUMED.toString()),
                new Item("test2.zip", envelope2.getCreatedAt(), null, "BULKSCAN", CONSUMED.toString())
            );
    }

    private void dbHasEvents(ProcessEvent... events) {
        eventRepo.saveAll(asList(events));
    }

    private void dbHasEnvelopes(Envelope... envelopes) {
        envelopeRepo.saveAll(asList(envelopes));
    }

    private Envelope envelope(String container, Envelope envelope) {
        envelope.setContainer(container);
        return envelope;
    }

    private ProcessEvent event(String container, String zipFileName, Instant createdAt, Event type) {
        ProcessEvent event = new ProcessEvent(container, zipFileName, type);
        event.setCreatedAt(createdAt);

        return event;
    }
}
