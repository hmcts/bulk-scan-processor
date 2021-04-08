package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.EnvelopeCountSummaryItem;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.EnvelopeCountSummaryRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.helper.reports.countsummary.Item;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static java.time.LocalDate.now;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.DOC_CONSUMED;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.DOC_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.DOC_PROCESSED_NOTIFICATION_SENT;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.DOC_UPLOADED;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.FILE_VALIDATION_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.ZIPFILE_PROCESSING_STARTED;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
@ExtendWith(SpringExtension.class)
public class EnvelopeCountSummaryRepositoryTest {

    @Autowired private EnvelopeCountSummaryRepository reportRepo;
    @Autowired private ProcessEventRepository eventRepo;

    @Test
    public void should_group_by_container() {
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
    public void should_filter_by_date() {
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
    public void should_count_zip_files_correctly_when_events_span_overnight() {
        // given

        Instant today = Instant.now();
        Instant yesterday = Instant.now().minus(1, ChronoUnit.DAYS);

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
    public void should_handle_multiple_events_per_zip_file() {
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
            event("service_E", "E1.zip", FILE_VALIDATION_FAILURE),
            event("service_E", "E1.zip", DOC_CONSUMED)
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
    public void should_handle_single_success_and_single_failure_per_zip_file() {
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

    private void dbHas(ProcessEvent... events) {
        eventRepo.saveAll(asList(events));
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
