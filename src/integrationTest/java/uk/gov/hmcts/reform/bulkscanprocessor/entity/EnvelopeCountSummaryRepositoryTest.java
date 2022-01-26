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
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static java.time.Instant.now;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.COMPLETED;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.DOC_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.DOC_PROCESSED_NOTIFICATION_SENT;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.DOC_PROCESSING_ABORTED;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.DOC_UPLOADED;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.FILE_VALIDATION_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.MANUAL_STATUS_CHANGE;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.ZIPFILE_PROCESSING_STARTED;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
@ExtendWith(SpringExtension.class)
class EnvelopeCountSummaryRepositoryTest {

    private static final LocalDate SUMMARY_DATE = LocalDate.parse("2021-07-02");

    @Autowired private EnvelopeCountSummaryRepository reportRepo;
    @Autowired private ProcessEventRepository eventRepo;
    @Autowired private EnvelopeRepository envelopeRepo;

    @Test
    void should_group_by_container() {
        // given
        Instant date = Instant.parse("2021-07-02T10:15:30Z");
        Instant previousDay = Instant.parse("2021-07-01T10:15:30Z");
        dbHas(
            event("A", Instant.parse("2021-07-02T10:15:30Z"), DOC_PROCESSED_NOTIFICATION_SENT),
            event("A", Instant.parse("2021-07-02T10:15:31Z"), DOC_PROCESSED_NOTIFICATION_SENT),
            event("A", Instant.parse("2021-07-02T10:15:32Z"), DOC_PROCESSED_NOTIFICATION_SENT),
            event("B", Instant.parse("2021-07-02T10:15:33Z"), DOC_PROCESSED_NOTIFICATION_SENT),
            event("B", Instant.parse("2021-07-02T10:15:34Z"), DOC_PROCESSED_NOTIFICATION_SENT),
            event("C", Instant.parse("2021-07-02T10:15:35Z"), DOC_PROCESSED_NOTIFICATION_SENT)
        );

        // when
        List<EnvelopeCountSummaryItem> result = reportRepo.getReportFor(SUMMARY_DATE);

        // then
        assertThat(result)
            .usingFieldByFieldElementComparator()
            .containsExactlyElementsOf(asList(
                new Item(SUMMARY_DATE, "A", 3, 0),
                new Item(SUMMARY_DATE, "B", 2, 0),
                new Item(SUMMARY_DATE, "C", 1, 0)
            ));
    }

    @Test
    void should_filter_by_date() {
        // given
        dbHas(
            event("X", Instant.parse("2021-07-02T10:15:30Z"), DOC_PROCESSED_NOTIFICATION_SENT),
            event("Y", Instant.parse("2021-07-02T10:15:31Z"), DOC_PROCESSED_NOTIFICATION_SENT)
        );

        // when
        List<EnvelopeCountSummaryItem> resultForYesterday = reportRepo.getReportFor(LocalDate.parse("2021-07-01"));

        // then
        assertThat(resultForYesterday).isEmpty();
    }

    @Test
    void should_count_zip_files_correctly_when_events_span_overnight() {
        // given

        dbHas(
            // originally parsed the day before
            event("some_service", "hello.zip", Instant.parse("2021-07-01T10:15:30Z"), DOC_FAILURE),
            // continuing the next day
            event("some_service", "hello.zip", Instant.parse("2021-07-02T10:15:30Z"), DOC_FAILURE),
            event("some_service", "hello.zip", Instant.parse("2021-07-02T10:15:31Z"), DOC_FAILURE),
            event("some_service", "hello.zip", Instant.parse("2021-07-02T10:15:32Z"), DOC_FAILURE)
        );

        // when
        List<EnvelopeCountSummaryItem> resultForToday = reportRepo.getReportFor(SUMMARY_DATE);

        // then
        assertThat(resultForToday).isEmpty();
    }

    @Test
    void should_handle_multiple_events_per_zip_file() {
        // given
        dbHas(
            event("service_A", "A1.zip", Instant.parse("2021-07-02T10:15:30Z"), DOC_UPLOADED),
            event("service_A", "A1.zip", Instant.parse("2021-07-02T10:15:31Z"), DOC_PROCESSED_NOTIFICATION_SENT),

            event("service_B", "B1.zip", Instant.parse("2021-07-02T10:15:32Z"), ZIPFILE_PROCESSING_STARTED),
            event("service_B", "B1.zip", Instant.parse("2021-07-02T10:15:33Z"), DOC_FAILURE),

            event("service_C", "C1.zip", Instant.parse("2021-07-02T10:15:34Z"), DOC_PROCESSED_NOTIFICATION_SENT),
            event("service_C", "C2.zip", Instant.parse("2021-07-02T10:15:35Z"), DOC_PROCESSED_NOTIFICATION_SENT),

            event("service_D", "D1.zip", Instant.parse("2021-07-02T10:15:36Z"), ZIPFILE_PROCESSING_STARTED),
            event("service_D", "D1.zip", Instant.parse("2021-07-02T10:15:37Z"), DOC_UPLOADED),
            event("service_D", "D1.zip", Instant.parse("2021-07-02T10:15:38Z"), DOC_PROCESSED_NOTIFICATION_SENT),
            event("service_D", "D2.zip", Instant.parse("2021-07-02T10:15:39Z"), ZIPFILE_PROCESSING_STARTED),
            event("service_D", "D2.zip", Instant.parse("2021-07-02T10:15:40Z"), FILE_VALIDATION_FAILURE),

            event("service_E", "E1.zip", Instant.parse("2021-07-02T10:15:41Z"), ZIPFILE_PROCESSING_STARTED),
            event("service_E", "E1.zip", Instant.parse("2021-07-02T10:15:42Z"), FILE_VALIDATION_FAILURE)
        );

        // when
        List<EnvelopeCountSummaryItem> result = reportRepo.getReportFor(SUMMARY_DATE);

        // then
        assertThat(result)
            .usingFieldByFieldElementComparator()
            .containsExactlyElementsOf(asList(
                new Item(SUMMARY_DATE, "service_A", 1, 0),
                new Item(SUMMARY_DATE, "service_B", 1, 1),
                new Item(SUMMARY_DATE, "service_C", 2, 0),
                new Item(SUMMARY_DATE, "service_D", 2, 1),
                new Item(SUMMARY_DATE, "service_E", 1, 1)
            ));
    }

    @Test
    void should_handle_single_success_and_single_failure_per_zip_file() {
        // given
        dbHas(
            event("service_A", "A1.zip", Instant.parse("2021-07-02T10:15:30Z"), DOC_UPLOADED),
            event("service_B", "B1.zip", Instant.parse("2021-07-02T10:15:31Z"), FILE_VALIDATION_FAILURE)
        );

        // when
        List<EnvelopeCountSummaryItem> result = reportRepo.getReportFor(SUMMARY_DATE);

        // then
        assertThat(result)
            .usingFieldByFieldElementComparator()
            .containsExactlyElementsOf(asList(
                new Item(SUMMARY_DATE, "service_A", 1, 0),
                new Item(SUMMARY_DATE, "service_B", 1, 1)
            ));
    }

    @Test
    void summary_report_should_group_by_container() {
        // given
        dbHas(
            event("A", Instant.parse("2021-07-02T10:15:30Z"), DOC_PROCESSED_NOTIFICATION_SENT),
            event("A", Instant.parse("2021-07-02T10:15:31Z"), DOC_PROCESSED_NOTIFICATION_SENT),
            event("A", Instant.parse("2021-07-02T10:15:32Z"), DOC_PROCESSED_NOTIFICATION_SENT),
            event("B", Instant.parse("2021-07-02T10:15:33Z"), DOC_PROCESSED_NOTIFICATION_SENT),
            event("B", Instant.parse("2021-07-02T10:15:34Z"), DOC_PROCESSED_NOTIFICATION_SENT),
            event("C", Instant.parse("2021-07-02T10:15:35Z"), DOC_PROCESSED_NOTIFICATION_SENT)
        );

        // when
        List<EnvelopeCountSummaryItem> result = reportRepo.getSummaryReportFor(SUMMARY_DATE);

        // then
        assertThat(result)
            .usingFieldByFieldElementComparator()
            .containsExactlyElementsOf(asList(
                new Item(SUMMARY_DATE, "A", 3, 0),
                new Item(SUMMARY_DATE, "B", 2, 0),
                new Item(SUMMARY_DATE, "C", 1, 0)
            ));
    }

    @Test
    void summary_report_should_filter_by_date() {
        // given
        dbHas(
            event("X", Instant.parse("2021-07-02T10:15:30Z"), DOC_PROCESSED_NOTIFICATION_SENT),
            event("Y", Instant.parse("2021-07-02T10:15:30Z"), DOC_PROCESSED_NOTIFICATION_SENT)
        );

        // when
        List<EnvelopeCountSummaryItem> resultForYesterday =
                reportRepo.getSummaryReportFor(LocalDate.parse("2021-07-01"));

        // then
        assertThat(resultForYesterday).isEmpty();
    }

    @Test
    void summary_report_should_handle_single_success_and_single_failure_per_zip_file() {
        // given
        dbHas(
            event("service_A", Instant.parse("2021-07-02T10:15:30Z"), DOC_UPLOADED),
            event("service_B", Instant.parse("2021-07-02T10:15:30Z"), FILE_VALIDATION_FAILURE)
        );

        // when
        List<EnvelopeCountSummaryItem> result = reportRepo.getSummaryReportFor(SUMMARY_DATE);

        // then
        assertThat(result)
            .usingFieldByFieldElementComparator()
            .containsExactlyElementsOf(asList(
                new Item(SUMMARY_DATE, "service_A", 1, 0),
                new Item(SUMMARY_DATE, "service_B", 1, 1)
            ));
    }

    @Test
    void summary_report_should_count_zip_files_correctly_when_no_envelopes() {
        // given

        dbHas(
                event("some_service", "hello.zip", Instant.parse("2021-07-02T10:15:30Z"), DOC_FAILURE),
                event("some_service", "hello.zip", Instant.parse("2021-07-02T10:15:31Z"), DOC_FAILURE),
                event("some_service", "hello.zip", Instant.parse("2021-07-02T10:15:32Z"), DOC_FAILURE)
        );

        // when
        List<EnvelopeCountSummaryItem> result = reportRepo.getReportFor(SUMMARY_DATE);

        // then
        assertThat(result)
                .usingFieldByFieldElementComparator()
                .containsExactlyElementsOf(singletonList(
                        new Item(SUMMARY_DATE, "some_service", 1, 1)
                ));
    }

    @Test
    void summary_report_should_handle_failure_and_subsequent_success() {
        // given
        dbHas(
                event("service_A", "A1.zip", Instant.parse("2021-07-02T10:15:30Z"), ZIPFILE_PROCESSING_STARTED),
                event("service_A", "A1.zip", Instant.parse("2021-07-02T10:15:31Z"), DOC_FAILURE),
                event("service_A", "A1.zip", Instant.parse("2021-07-02T10:15:32Z"), ZIPFILE_PROCESSING_STARTED),
                event("service_A", "A1.zip", Instant.parse("2021-07-02T10:15:33Z"), DOC_UPLOADED),
                event("service_A", "A1.zip", Instant.parse("2021-07-02T10:15:34Z"), DOC_PROCESSED_NOTIFICATION_SENT),
                event("service_A", "A1.zip", Instant.parse("2021-07-02T10:15:35Z"), COMPLETED),

                event("service_B", "B1.zip", Instant.parse("2021-07-02T10:15:36Z"), ZIPFILE_PROCESSING_STARTED),
                event("service_B", "B1.zip", Instant.parse("2021-07-02T10:15:37Z"), FILE_VALIDATION_FAILURE)
        );

        // when
        List<EnvelopeCountSummaryItem> result = reportRepo.getSummaryReportFor(SUMMARY_DATE);

        // then
        assertThat(result)
                .usingFieldByFieldElementComparator()
                .containsExactlyElementsOf(asList(
                        new Item(SUMMARY_DATE, "service_A", 1, 0),
                        new Item(SUMMARY_DATE, "service_B", 1, 1)
                ));
    }

    @Test
    void summary_report_should_handle_failure_and_subsequent_success_after_several_days() {
        // given
        dbHas(
                event("service_A", "A1.zip", Instant.parse("2021-07-02T10:15:30Z"), ZIPFILE_PROCESSING_STARTED),
                event("service_A", "A1.zip", Instant.parse("2021-07-02T10:15:31Z"), DOC_FAILURE),
                event("service_A", "A1.zip", Instant.parse("2021-07-02T10:15:32Z"), ZIPFILE_PROCESSING_STARTED),
                event("service_A", "A1.zip", Instant.parse("2021-07-02T10:15:33Z"), DOC_UPLOADED),
                event("service_A", "A1.zip", Instant.parse("2021-07-02T10:15:34Z"), DOC_PROCESSED_NOTIFICATION_SENT),
                // two days later
                event("service_A", "A1.zip", Instant.parse("2021-07-04T10:15:30Z"), COMPLETED),

                event("service_B", "B1.zip", Instant.parse("2021-07-02T10:15:36Z"), ZIPFILE_PROCESSING_STARTED),
                event("service_B", "B1.zip", Instant.parse("2021-07-02T10:15:37Z"), FILE_VALIDATION_FAILURE)
        );

        // when
        List<EnvelopeCountSummaryItem> result = reportRepo.getSummaryReportFor(SUMMARY_DATE);

        // then
        assertThat(result)
                .usingFieldByFieldElementComparator()
                .containsExactlyElementsOf(asList(
                        new Item(SUMMARY_DATE, "service_A", 1, 0),
                        new Item(SUMMARY_DATE, "service_B", 1, 1)
                ));
    }

    @Test
    void summary_report_should_handle_multiple_failures_and_subsequent_success() {
        // given
        dbHas(
                event("service_A", "A1.zip", Instant.parse("2021-07-02T10:15:30Z"), ZIPFILE_PROCESSING_STARTED),
                event("service_A", "A1.zip", Instant.parse("2021-07-02T10:15:31Z"), DOC_FAILURE),
                event("service_A", "A1.zip", Instant.parse("2021-07-02T10:15:32Z"), ZIPFILE_PROCESSING_STARTED),
                event("service_A", "A1.zip", Instant.parse("2021-07-02T10:15:33Z"), DOC_FAILURE),
                event("service_A", "A1.zip", Instant.parse("2021-07-02T10:15:34Z"), ZIPFILE_PROCESSING_STARTED),
                event("service_A", "A1.zip", Instant.parse("2021-07-02T10:15:35Z"), DOC_FAILURE),
                event("service_A", "A1.zip", Instant.parse("2021-07-02T10:15:36Z"), ZIPFILE_PROCESSING_STARTED),
                event("service_A", "A1.zip", Instant.parse("2021-07-02T10:15:37Z"), DOC_UPLOADED),
                event("service_A", "A1.zip", Instant.parse("2021-07-02T10:15:38Z"), DOC_PROCESSED_NOTIFICATION_SENT),
                event("service_A", "A1.zip", Instant.parse("2021-07-02T10:15:39Z"), COMPLETED),

                event("service_B", "B1.zip", Instant.parse("2021-07-02T10:15:40Z"), ZIPFILE_PROCESSING_STARTED),
                event("service_B", "B1.zip", Instant.parse("2021-07-02T10:15:41Z"), FILE_VALIDATION_FAILURE)
        );

        // when
        List<EnvelopeCountSummaryItem> result = reportRepo.getSummaryReportFor(SUMMARY_DATE);

        // then
        assertThat(result)
                .usingFieldByFieldElementComparator()
                .containsExactlyElementsOf(asList(
                        new Item(SUMMARY_DATE, "service_A", 1, 0),
                        new Item(SUMMARY_DATE, "service_B", 1, 1)
                ));
    }

    @Test
    void summary_report_should_handle_multiple_failures_and_subsequent_success_after_several_days() {
        // given
        dbHas(
                event("service_A", "A1.zip", Instant.parse("2021-07-02T10:15:30Z"), ZIPFILE_PROCESSING_STARTED),
                event("service_A", "A1.zip", Instant.parse("2021-07-02T10:15:31Z"), DOC_FAILURE),
                event("service_A", "A1.zip", Instant.parse("2021-07-02T10:15:32Z"), ZIPFILE_PROCESSING_STARTED),
                event("service_A", "A1.zip", Instant.parse("2021-07-02T10:15:33Z"), DOC_FAILURE),
                event("service_A", "A1.zip", Instant.parse("2021-07-02T10:15:34Z"), ZIPFILE_PROCESSING_STARTED),
                event("service_A", "A1.zip", Instant.parse("2021-07-02T10:15:35Z"), DOC_FAILURE),
                event("service_A", "A1.zip", Instant.parse("2021-07-02T10:15:36Z"), ZIPFILE_PROCESSING_STARTED),
                event("service_A", "A1.zip", Instant.parse("2021-07-02T10:15:37Z"), DOC_UPLOADED),
                event("service_A", "A1.zip", Instant.parse("2021-07-02T10:15:38Z"), DOC_PROCESSED_NOTIFICATION_SENT),
                // ten days later
                event("service_A", "A1.zip", Instant.parse("2021-07-12T10:15:30Z"), COMPLETED),

                event("service_B", "B1.zip", Instant.parse("2021-07-02T10:15:40Z"), ZIPFILE_PROCESSING_STARTED),
                event("service_B", "B1.zip", Instant.parse("2021-07-02T10:15:41Z"), FILE_VALIDATION_FAILURE)
        );

        // when
        List<EnvelopeCountSummaryItem> result = reportRepo.getSummaryReportFor(SUMMARY_DATE);

        // then
        assertThat(result)
                .usingFieldByFieldElementComparator()
                .containsExactlyElementsOf(asList(
                        new Item(SUMMARY_DATE, "service_A", 1, 0),
                        new Item(SUMMARY_DATE, "service_B", 1, 1)
                ));
    }

    @Test
    void summary_report_should_handle_manual_status_change() {
        // given
        dbHas(
                event("service_A", "A1.zip", Instant.parse("2021-07-02T10:15:30Z"), ZIPFILE_PROCESSING_STARTED),
                event("service_A", "A1.zip", Instant.parse("2021-07-02T10:15:31Z"), DOC_UPLOADED),
                event("service_A", "A1.zip", Instant.parse("2021-07-02T10:15:32Z"), DOC_PROCESSED_NOTIFICATION_SENT),
                event("service_A", "A1.zip", Instant.parse("2021-07-02T10:15:33Z"), MANUAL_STATUS_CHANGE),

                event("service_B", "B1.zip", Instant.parse("2021-07-02T10:15:34Z"), ZIPFILE_PROCESSING_STARTED),
                event("service_B", "B1.zip", Instant.parse("2021-07-02T10:15:35Z"), FILE_VALIDATION_FAILURE)
        );

        // when
        List<EnvelopeCountSummaryItem> result = reportRepo.getSummaryReportFor(SUMMARY_DATE);

        // then
        assertThat(result)
                .usingFieldByFieldElementComparator()
                .containsExactlyElementsOf(asList(
                        new Item(SUMMARY_DATE, "service_A", 1, 0),
                        new Item(SUMMARY_DATE, "service_B", 1, 1)
                ));
    }

    @Test
    void summary_report_should_handle_manual_status_change_after_several_days() {
        // given
        dbHas(
                event("service_A", "A1.zip", Instant.parse("2021-07-02T10:15:30Z"), ZIPFILE_PROCESSING_STARTED),
                event("service_A", "A1.zip", Instant.parse("2021-07-02T10:15:31Z"), DOC_UPLOADED),
                event("service_A", "A1.zip", Instant.parse("2021-07-02T10:15:32Z"), DOC_PROCESSED_NOTIFICATION_SENT),
                // ten days later
                event("service_A", "A1.zip", Instant.parse("2021-07-12T10:15:30Z"), MANUAL_STATUS_CHANGE),

                event("service_B", "B1.zip", Instant.parse("2021-07-02T10:15:34Z"), ZIPFILE_PROCESSING_STARTED),
                event("service_B", "B1.zip", Instant.parse("2021-07-02T10:15:35Z"), FILE_VALIDATION_FAILURE)
        );

        // when
        List<EnvelopeCountSummaryItem> result = reportRepo.getSummaryReportFor(SUMMARY_DATE);

        // then
        assertThat(result)
                .usingFieldByFieldElementComparator()
                .containsExactlyElementsOf(asList(
                        new Item(SUMMARY_DATE, "service_A", 1, 0),
                        new Item(SUMMARY_DATE, "service_B", 1, 1)
                ));
    }

    @Test
    void summary_report_should_handle_doc_processing_aborted() {
        // given
        dbHas(
                event("service_A", "A1.zip", Instant.parse("2021-07-02T10:15:30Z"), ZIPFILE_PROCESSING_STARTED),
                event("service_A", "A1.zip", Instant.parse("2021-07-02T10:15:31Z"), DOC_UPLOADED),
                event("service_A", "A1.zip", Instant.parse("2021-07-02T10:15:32Z"), DOC_PROCESSED_NOTIFICATION_SENT),
                event("service_A", "A1.zip", Instant.parse("2021-07-02T10:15:33Z"), DOC_PROCESSING_ABORTED),

                event("service_B", "B1.zip", Instant.parse("2021-07-02T10:15:34Z"), ZIPFILE_PROCESSING_STARTED),
                event("service_B", "B1.zip", Instant.parse("2021-07-02T10:15:35Z"), FILE_VALIDATION_FAILURE)
        );

        // when
        List<EnvelopeCountSummaryItem> result = reportRepo.getSummaryReportFor(SUMMARY_DATE);

        // then
        assertThat(result)
                .usingFieldByFieldElementComparator()
                .containsExactlyElementsOf(asList(
                        new Item(SUMMARY_DATE, "service_A", 1, 0),
                        new Item(SUMMARY_DATE, "service_B", 1, 1)
                ));
    }

    @Test
    void summary_report_should_handle_doc_processing_aborted_after_several_days() {
        // given
        dbHas(
                event("service_A", "A1.zip", Instant.parse("2021-07-02T10:15:30Z"), ZIPFILE_PROCESSING_STARTED),
                event("service_A", "A1.zip", Instant.parse("2021-07-02T10:15:31Z"), DOC_UPLOADED),
                event("service_A", "A1.zip", Instant.parse("2021-07-02T10:15:32Z"), DOC_PROCESSED_NOTIFICATION_SENT),
                // two days later
                event("service_A", "A1.zip", Instant.parse("2021-07-04T10:15:30Z"), DOC_PROCESSING_ABORTED),

                event("service_B", "B1.zip", Instant.parse("2021-07-02T10:15:34Z"), ZIPFILE_PROCESSING_STARTED),
                event("service_B", "B1.zip", Instant.parse("2021-07-02T10:15:35Z"), FILE_VALIDATION_FAILURE)
        );

        // when
        List<EnvelopeCountSummaryItem> result = reportRepo.getSummaryReportFor(SUMMARY_DATE);

        // then
        assertThat(result)
                .usingFieldByFieldElementComparator()
                .containsExactlyElementsOf(asList(
                        new Item(SUMMARY_DATE, "service_A", 1, 0),
                        new Item(SUMMARY_DATE, "service_B", 1, 1)
                ));
    }

    @Test
    void envelope_summary_report_should_provide_counts_for_date() {
        // given
        dbHas(
                envelope("service_A", Status.COMPLETED, Instant.parse("2021-07-02T10:15:30Z")),
                envelope("service_A", Status.METADATA_FAILURE, Instant.parse("2021-07-02T10:15:31Z")),
                envelope("service_B", Status.COMPLETED, Instant.parse("2021-07-02T10:15:32Z")),
                envelope("service_B", Status.COMPLETED, Instant.parse("2021-07-02T10:15:33Z")),
                envelope("service_B", Status.UPLOAD_FAILURE, Instant.parse("2021-07-02T10:15:34Z")),
                envelope("service_B", Status.NOTIFICATION_SENT, Instant.parse("2021-07-02T10:15:35Z")),

                // next day
                envelope("service_B", Status.COMPLETED, Instant.parse("2021-07-03T10:15:36Z"))
        );

        // when
        List<EnvelopeCountSummaryItem> result = reportRepo.getEnvelopeCountSummary(SUMMARY_DATE);

        // then
        assertThat(result)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyElementsOf(asList(
                        new Item(SUMMARY_DATE, "service_A", 2, 1),
                        new Item(SUMMARY_DATE, "service_B", 4, 1)
                ));
    }

    private void dbHas(ProcessEvent... events) {
        eventRepo.saveAll(asList(events));
    }

    private void dbHas(Envelope... envelopes) {
        envelopeRepo.saveAll(asList(envelopes));
    }

    private Envelope envelope(String container, Status status, Instant zipfilecreateddate) {
        Envelope envelope = new Envelope(
                "SSCSPO",
                "X",
                now(),
                now(),
                zipfilecreateddate,
                randomUUID() + ".zip",
                "1111222233334446",
                "123654789",
                Classification.EXCEPTION,
                emptyList(),
                emptyList(),
                emptyList(),
                container,
                null
        );
        envelope.setStatus(status);
        return envelope;
    }

    private ProcessEvent event(String container, Instant createdAt, Event type) {
        return event(container, UUID.randomUUID().toString(), createdAt, type);
    }

    private ProcessEvent event(String container, String zipFileName, Instant createdAt, Event type) {
        ProcessEvent event = new ProcessEvent(container, zipFileName, type);
        event.setCreatedAt(createdAt);

        return event;
    }
}
