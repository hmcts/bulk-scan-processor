package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.EnvelopeCountSummaryItem;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.EnvelopeCountSummaryRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.helper.EnvelopeCreator;
import uk.gov.hmcts.reform.bulkscanprocessor.helper.reports.countsummary.Item;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static java.time.LocalDate.now;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.DOC_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.DOC_PROCESSED_NOTIFICATION_SENT;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.DOC_UPLOADED;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.FILE_VALIDATION_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.ZIPFILE_PROCESSING_STARTED;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class EnvelopeCountSummaryRepositoryTest {

    @Autowired private EnvelopeCountSummaryRepository reportRepo;
    @Autowired private EnvelopeRepository envelopeRepo;
    @Autowired private ProcessEventRepository eventRepo;

    @Test
    void should_group_by_container() {
        // given
        dbHas(
            event("A", DOC_PROCESSED_NOTIFICATION_SENT),
            event("A", DOC_PROCESSED_NOTIFICATION_SENT),
            event("A", DOC_PROCESSED_NOTIFICATION_SENT),
            event("B", DOC_PROCESSED_NOTIFICATION_SENT),
            event("B", DOC_PROCESSED_NOTIFICATION_SENT),
            event("C", DOC_PROCESSED_NOTIFICATION_SENT)
        );

        // when
        List<EnvelopeCountSummaryItem> result = reportRepo.getReportFor(now());

        // then
        assertThat(result)
            .usingFieldByFieldElementComparator()
            .containsExactlyElementsOf(asList(
                new Item(now(), "A", 3, 0),
                new Item(now(), "B", 2, 0),
                new Item(now(), "C", 1, 0)
            ));
    }

    @Test
    void should_filter_by_date() {
        // given
        dbHas(
            event("X", DOC_PROCESSED_NOTIFICATION_SENT),
            event("Y", DOC_PROCESSED_NOTIFICATION_SENT)
        );

        // when
        List<EnvelopeCountSummaryItem> resultForYesterday = reportRepo.getReportFor(now().minusDays(1));

        // then
        assertThat(resultForYesterday).isEmpty();
    }

    @Test
    void should_count_zip_files_correctly_when_events_span_overnight() {
        // given

        Instant today = Instant.now();
        Instant yesterday = Instant.now().minus(1, DAYS);

        dbHas(
            event("some_service", "hello.zip", yesterday, DOC_FAILURE), // originally parsed yesterday
            event("some_service", "hello.zip", today, DOC_FAILURE),
            event("some_service", "hello.zip", today, DOC_FAILURE),
            event("some_service", "hello.zip", today, DOC_FAILURE)
        );

        // when
        List<EnvelopeCountSummaryItem> resultForToday = reportRepo.getReportFor(now());

        // then
        assertThat(resultForToday).isEmpty();
    }

    @Test
    void should_handle_multiple_events_per_zip_file() {
        // given
        dbHas(
            event("service_A", "A1.zip", DOC_UPLOADED),
            event("service_A", "A1.zip", DOC_PROCESSED_NOTIFICATION_SENT),

            event("service_B", "B1.zip", ZIPFILE_PROCESSING_STARTED),
            event("service_B", "B1.zip", DOC_FAILURE),

            event("service_C", "C1.zip", DOC_PROCESSED_NOTIFICATION_SENT),
            event("service_C", "C2.zip", DOC_PROCESSED_NOTIFICATION_SENT),

            event("service_D", "D1.zip", ZIPFILE_PROCESSING_STARTED),
            event("service_D", "D1.zip", DOC_UPLOADED),
            event("service_D", "D1.zip", DOC_PROCESSED_NOTIFICATION_SENT),
            event("service_D", "D2.zip", ZIPFILE_PROCESSING_STARTED),
            event("service_D", "D2.zip", FILE_VALIDATION_FAILURE),

            event("service_E", "E1.zip", ZIPFILE_PROCESSING_STARTED),
            event("service_E", "E1.zip", FILE_VALIDATION_FAILURE)
        );

        // when
        List<EnvelopeCountSummaryItem> result = reportRepo.getReportFor(now());

        // then
        assertThat(result)
            .usingFieldByFieldElementComparator()
            .containsExactlyElementsOf(asList(
                new Item(now(), "service_A", 1, 0),
                new Item(now(), "service_B", 1, 1),
                new Item(now(), "service_C", 2, 0),
                new Item(now(), "service_D", 2, 1),
                new Item(now(), "service_E", 1, 1)
            ));
    }

    @Test
    void should_handle_single_success_and_single_failure_per_zip_file() {
        // given
        dbHas(
            event("service_A", "A1.zip", DOC_UPLOADED),
            event("service_B", "B1.zip", FILE_VALIDATION_FAILURE)
        );

        // when
        List<EnvelopeCountSummaryItem> result = reportRepo.getReportFor(now());

        // then
        assertThat(result)
            .usingFieldByFieldElementComparator()
            .containsExactlyElementsOf(asList(
                new Item(now(), "service_A", 1, 0),
                new Item(now(), "service_B", 1, 1)
            ));
    }

    @Test
    void summary_report_should_group_by_container() {
        // given
        Envelope envelope1 = envelope("A");
        Envelope envelope2 = envelope("A");
        Envelope envelope3 = envelope("A");
        Envelope envelope4 = envelope("B");
        Envelope envelope5 = envelope("B");
        Envelope envelope6 = envelope("C");
        dbHas(
                envelope1,
                envelope2,
                envelope3,
                envelope4,
                envelope5,
                envelope6
        );
        dbHas(
            event("A", envelope1.getZipFileName(), DOC_PROCESSED_NOTIFICATION_SENT),
            event("A", envelope2.getZipFileName(), DOC_PROCESSED_NOTIFICATION_SENT),
            event("A", envelope3.getZipFileName(), DOC_PROCESSED_NOTIFICATION_SENT),
            event("B", envelope4.getZipFileName(), DOC_PROCESSED_NOTIFICATION_SENT),
            event("B", envelope5.getZipFileName(), DOC_PROCESSED_NOTIFICATION_SENT),
            event("C", envelope6.getZipFileName(), DOC_PROCESSED_NOTIFICATION_SENT)
        );

        // when
        List<EnvelopeCountSummaryItem> result = reportRepo.getSummaryReportFor(now());

        // then
        assertThat(result)
            .usingFieldByFieldElementComparator()
            .containsExactlyElementsOf(asList(
                new Item(now(), "A", 3, 0),
                new Item(now(), "B", 2, 0),
                new Item(now(), "C", 1, 0)
            ));
    }

    @Test
    void summary_report_should_filter_by_date() {
        // given
        Envelope envelope1 = envelope("X");
        Envelope envelope2 = envelope("Y");
        dbHas(
                envelope1,
                envelope2
        );
        dbHas(
            event("X", envelope1.getZipFileName(), DOC_PROCESSED_NOTIFICATION_SENT),
            event("Y", envelope2.getZipFileName(), DOC_PROCESSED_NOTIFICATION_SENT)
        );

        // when
        List<EnvelopeCountSummaryItem> resultForYesterday = reportRepo.getSummaryReportFor(now().minusDays(1));

        // then
        assertThat(resultForYesterday).isEmpty();
    }

    @Test
    void summary_report_should_handle_single_success_and_single_failure_per_zip_file() {
        // given
        // given
        Envelope envelope1 = envelope("service_A");
        Envelope envelope2 = envelope("service_B");
        dbHas(
                envelope1,
                envelope2
        );
        dbHas(
            event("service_A", envelope1.getZipFileName(), DOC_UPLOADED),
            event("service_B", envelope2.getZipFileName(), FILE_VALIDATION_FAILURE)
        );

        // when
        List<EnvelopeCountSummaryItem> result = reportRepo.getSummaryReportFor(now());

        // then
        assertThat(result)
            .usingFieldByFieldElementComparator()
            .containsExactlyElementsOf(asList(
                new Item(now(), "service_A", 1, 0),
                new Item(now(), "service_B", 1, 1)
            ));
    }

    @Test
    void summary_report_should_count_zip_files_correctly_when_no_envelopes() {
        // given

        Instant today = Instant.now();

        dbHas(
                event("some_service", "hello.zip", today, DOC_FAILURE),
                event("some_service", "hello.zip", today, DOC_FAILURE),
                event("some_service", "hello.zip", today, DOC_FAILURE)
        );

        // when
        List<EnvelopeCountSummaryItem> result = reportRepo.getReportFor(now());

        // then
        assertThat(result)
                .usingFieldByFieldElementComparator()
                .containsExactlyElementsOf(singletonList(
                        new Item(now(), "some_service", 1, 1)
                ));
    }

    private void dbHas(Envelope... envelopes) {
        envelopeRepo.saveAll(asList(envelopes));
    }

    private void dbHas(ProcessEvent... events) {
        eventRepo.saveAll(asList(events));
    }

    private Envelope envelope(String container) {
        return EnvelopeCreator.envelope("X", Status.COMPLETED, container);
    }

    private ProcessEvent event(String container, Event type) {
        return event(container, UUID.randomUUID().toString(), type);
    }

    private ProcessEvent event(String container, String zipFileName, Event type) {
        return event(container, zipFileName, Instant.now(), type);
    }

    private ProcessEvent event(String container, String zipFileName, Instant createdAt, Event type) {
        ProcessEvent event = new ProcessEvent(container, zipFileName, type);
        event.setCreatedAt(createdAt);

        return event;
    }
}
