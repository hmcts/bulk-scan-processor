package uk.gov.hmcts.reform.bulkscanprocessor.services.reports;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.EnvelopeCountSummaryRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.ZipFilesSummaryRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.helper.reports.countsummary.Item;
import uk.gov.hmcts.reform.bulkscanprocessor.helper.reports.zipfilesummary.ZipFileSummaryItem;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.EnvelopeCountSummary;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.ZipFileSummaryResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.utils.ZeroRowFiller;

import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static java.time.LocalDate.now;
import static java.time.LocalDateTime.ofInstant;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification.EXCEPTION;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification.NEW_APPLICATION;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.COMPLETED;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.ZIPFILE_PROCESSING_STARTED;
import static uk.gov.hmcts.reform.bulkscanprocessor.services.reports.ReportsService.TEST_CONTAINER;
import static uk.gov.hmcts.reform.bulkscanprocessor.util.TimeZones.EUROPE_LONDON_ZONE_ID;

@ExtendWith(MockitoExtension.class)
class ReportsServiceTest {

    @Mock private EnvelopeCountSummaryRepository repo;
    @Mock private ZeroRowFiller zeroRowFiller;
    @Mock private ZipFilesSummaryRepository zipFilesSummaryRepo;

    private ReportsService service;

    @BeforeEach
    void setUp() {
        this.service = new ReportsService(this.repo, zeroRowFiller, zipFilesSummaryRepo);
    }

    @Test
    void should_map_repo_result_properly_when_requested_for_envelope_count_summary() {
        given(repo.getReportFor(now()))
            .willReturn(asList(
                new Item(now().plusDays(1), "A", 100, 1),
                new Item(now().minusDays(1), "B", 200, 9)
            ));
        given(this.zeroRowFiller.fill(any(), any()))
            .willAnswer(invocation -> invocation.getArgument(0)); // return data unchanged

        // when
        List<EnvelopeCountSummary> result = service.getCountFor(now(), false);

        // then
        assertThat(result)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactly(
                new EnvelopeCountSummary(100, 1, "A", now().plusDays(1)),
                new EnvelopeCountSummary(200, 9, "B", now().minusDays(1))
            );
    }

    @Test
    void should_filter_out_test_container_when_requested_for_envelope_count_summary() {
        given(repo.getReportFor(now()))
            .willReturn(asList(
                new Item(now(), TEST_CONTAINER, 100, 1),
                new Item(now(), "some_other_container", 10, 0)
            ));
        given(this.zeroRowFiller.fill(any(), any()))
            .willAnswer(invocation -> invocation.getArgument(0)); // return data unchanged

        // when
        List<EnvelopeCountSummary> resultWithoutTestContainer = service.getCountFor(now(), false);
        List<EnvelopeCountSummary> resultWithTestContainer = service.getCountFor(now(), true);

        // then
        assertThat(resultWithoutTestContainer).hasSize(1);
        assertThat(resultWithoutTestContainer.get(0).container).isEqualTo("some_other_container");

        assertThat(resultWithTestContainer).hasSize(2);
    }

    @Test
    void should_map_empty_list_from_repo_when_requested_for_envelope_count_summary() {
        given(repo.getReportFor(now())).willReturn(emptyList());
        given(this.zeroRowFiller.fill(any(), any()))
            .willAnswer(invocation -> invocation.getArgument(0)); // return data unchanged

        // when
        List<EnvelopeCountSummary> result = service.getCountFor(now(), false);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void summary_report_should_map_repo_result_properly_when_requested_for_envelope_count_summary() {
        given(repo.getSummaryReportFor(now()))
            .willReturn(asList(
                new Item(now().plusDays(1), "A", 100, 1),
                new Item(now().minusDays(1), "B", 200, 9)
            ));
        given(this.zeroRowFiller.fill(any(), any()))
            .willAnswer(invocation -> invocation.getArgument(0)); // return data unchanged

        // when
        List<EnvelopeCountSummary> result = service.getSummaryCountFor(now(), false);

        // then
        assertThat(result)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactly(
                new EnvelopeCountSummary(100, 1, "A", now().plusDays(1)),
                new EnvelopeCountSummary(200, 9, "B", now().minusDays(1))
            );
    }

    @Test
    void summary_report_should_filter_out_test_container_when_requested_for_envelope_count_summary() {
        given(repo.getSummaryReportFor(now()))
            .willReturn(asList(
                new Item(now(), TEST_CONTAINER, 100, 1),
                new Item(now(), "some_other_container", 10, 0)
            ));
        given(this.zeroRowFiller.fill(any(), any()))
            .willAnswer(invocation -> invocation.getArgument(0)); // return data unchanged

        // when
        List<EnvelopeCountSummary> resultWithoutTestContainer = service.getSummaryCountFor(now(), false);
        List<EnvelopeCountSummary> resultWithTestContainer = service.getSummaryCountFor(now(), true);

        // then
        assertThat(resultWithoutTestContainer).hasSize(1);
        assertThat(resultWithoutTestContainer.get(0).container).isEqualTo("some_other_container");

        assertThat(resultWithTestContainer).hasSize(2);
    }

    @Test
    void summary_report_should_map_empty_list_from_repo_when_requested_for_envelope_count_summary() {
        given(repo.getSummaryReportFor(now())).willReturn(emptyList());
        given(this.zeroRowFiller.fill(any(), any()))
            .willAnswer(invocation -> invocation.getArgument(0)); // return data unchanged

        // when
        List<EnvelopeCountSummary> result = service.getSummaryCountFor(now(), false);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void should_map_empty_list_from_repo_when_requested_for_zipfiles_summary() {
        given(zipFilesSummaryRepo.getZipFileSummaryReportFor(now()))
            .willReturn(emptyList());

        // when
        List<ZipFileSummaryResponse> result = service.getZipFilesSummary(now(), null, null);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void should_map_db_results_when_requested_for_zipfiles_summary() {
        Instant instant = Instant.now();
        given(zipFilesSummaryRepo.getZipFileSummaryReportFor(now()))
            .willReturn(asList(
                new ZipFileSummaryItem(
                    "t1.zip",
                    instant.minus(1, MINUTES),
                    null,
                    "c1",
                    Event.ZIPFILE_PROCESSING_STARTED.toString(),
                    Status.CREATED.toString(),
                    EXCEPTION.name(),
                    null,
                    null,
                    null
                ),
                new ZipFileSummaryItem(
                    "t2.zip",
                    instant.minus(10, MINUTES),
                    instant.minus(20, MINUTES),
                    "c2",
                    Event.COMPLETED.toString(),
                    Status.UPLOADED.toString(),
                    NEW_APPLICATION.name(),
                    "ccd-id",
                    "ccd-action",
                    null
                )
            ));

        // when
        List<ZipFileSummaryResponse> result = service.getZipFilesSummary(now(), null, null);

        // then
        assertThat(result)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactly(
                new ZipFileSummaryResponse(
                    "t1.zip",
                    ofInstant(instant.minus(1, MINUTES), EUROPE_LONDON_ZONE_ID).toLocalDate(),
                    toLocalTime(instant.minus(1, MINUTES)),
                    null,
                    null,
                    "c1",
                    Event.ZIPFILE_PROCESSING_STARTED.toString(),
                    Status.CREATED.toString(),
                    EXCEPTION.name(),
                    null,
                    null
                ),
                new ZipFileSummaryResponse(
                    "t2.zip",
                    ofInstant(instant.minus(10, MINUTES), EUROPE_LONDON_ZONE_ID).toLocalDate(),
                    toLocalTime(instant.minus(10, MINUTES)),
                    ofInstant(instant.minus(20, MINUTES), EUROPE_LONDON_ZONE_ID).toLocalDate(),
                    toLocalTime(instant.minus(20, MINUTES)),
                    "c2",
                    Event.COMPLETED.toString(),
                    Status.UPLOADED.toString(),
                    NEW_APPLICATION.name(),
                    "ccd-id",
                    "ccd-action"
                )
            );
    }

    @Test
    void should_filter_zipfiles_by_container_when_requested_for_zipfiles_summary() {
        Instant instant = Instant.now();
        given(zipFilesSummaryRepo.getZipFileSummaryReportFor(now()))
            .willReturn(asList(
                new ZipFileSummaryItem(
                    "t1.zip",
                    instant.minus(1, MINUTES),
                    null,
                    "c1",
                    ZIPFILE_PROCESSING_STARTED.toString(),
                    null,
                    EXCEPTION.name(),
                    null,
                    null,
                    null
                ),
                new ZipFileSummaryItem(
                    "t2.zip",
                    instant.minus(10, MINUTES),
                    instant.minus(20, MINUTES),
                    "c2",
                    COMPLETED.toString(),
                    null,
                    EXCEPTION.name(),
                    null,
                    null,
                    null
                )
            ));

        // when
        List<ZipFileSummaryResponse> result = service.getZipFilesSummary(now(), "c2", null);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).container).isEqualTo("c2");
        assertThat(result.get(0).fileName).isEqualTo("t2.zip");
        assertThat(result.get(0).classification).isEqualTo(EXCEPTION.name());
    }

    @Test
    void should_filter_zipfiles_by_classification_when_requested_for_zipfiles_summary() {
        Instant instant = Instant.now();
        given(zipFilesSummaryRepo.getZipFileSummaryReportFor(now()))
            .willReturn(asList(
                new ZipFileSummaryItem(
                    "t1.zip",
                    instant.minus(1, MINUTES),
                    null,
                    "c1",
                    ZIPFILE_PROCESSING_STARTED.toString(),
                    null,
                    EXCEPTION.name(),
                    null,
                    null,
                    null
                ),
                new ZipFileSummaryItem(
                    "t2.zip",
                    instant.minus(1, MINUTES),
                    null,
                    "c1",
                    ZIPFILE_PROCESSING_STARTED.toString(),
                    null,
                    NEW_APPLICATION.name(),
                    null,
                    null,
                    null
                ),
                new ZipFileSummaryItem(
                    "t3.zip",
                    instant.minus(10, MINUTES),
                    instant.minus(20, MINUTES),
                    "c2",
                    COMPLETED.toString(),
                    null,
                    EXCEPTION.name(),
                    null,
                    null,
                    null
                ),
                new ZipFileSummaryItem(
                    "t4.zip",
                    instant.minus(10, MINUTES),
                    instant.minus(20, MINUTES),
                    "c2",
                    COMPLETED.toString(),
                    null,
                    NEW_APPLICATION.name(),
                    null,
                    null,
                    null
                )
            ));

        // when
        List<ZipFileSummaryResponse> result = service.getZipFilesSummary(now(), null, NEW_APPLICATION);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).container).isEqualTo("c1");
        assertThat(result.get(0).fileName).isEqualTo("t2.zip");
        assertThat(result.get(0).classification).isEqualTo(NEW_APPLICATION.name());
        assertThat(result.get(1).container).isEqualTo("c2");
        assertThat(result.get(1).fileName).isEqualTo("t4.zip");
        assertThat(result.get(1).classification).isEqualTo(NEW_APPLICATION.name());
    }

    @Test
    void should_filter_zipfiles_by_container_and_classification_when_requested_for_zipfiles_summary() {
        Instant instant = Instant.now();
        given(zipFilesSummaryRepo.getZipFileSummaryReportFor(now()))
            .willReturn(asList(
                new ZipFileSummaryItem(
                    "t1.zip",
                    instant.minus(1, MINUTES),
                    null,
                    "c1",
                    ZIPFILE_PROCESSING_STARTED.toString(),
                    null,
                    EXCEPTION.name(),
                    null,
                    null,
                    null
                ),
                new ZipFileSummaryItem(
                    "t2.zip",
                    instant.minus(1, MINUTES),
                    null,
                    "c1",
                    ZIPFILE_PROCESSING_STARTED.toString(),
                    null,
                    NEW_APPLICATION.name(),
                    null,
                    null,
                    null
                ),
                new ZipFileSummaryItem(
                    "t3.zip",
                    instant.minus(10, MINUTES),
                    instant.minus(20, MINUTES),
                    "c2",
                    COMPLETED.toString(),
                    null,
                    EXCEPTION.name(),
                    null,
                    null,
                    null
                ),
                new ZipFileSummaryItem(
                    "t4.zip",
                    instant.minus(10, MINUTES),
                    instant.minus(20, MINUTES),
                    "c2",
                    COMPLETED.toString(),
                    null,
                    NEW_APPLICATION.name(),
                    null,
                    null,
                    null
                )
            ));

        // when
        List<ZipFileSummaryResponse> result = service.getZipFilesSummary(now(), "c2", NEW_APPLICATION);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).container).isEqualTo("c2");
        assertThat(result.get(0).fileName).isEqualTo("t4.zip");
        assertThat(result.get(0).classification).isEqualTo(NEW_APPLICATION.name());
    }

    @Test
    void envelope_summary_count_report_should_map_repo_result_properly_when_requested_for_envelope_count_summary() {
        given(repo.getEnvelopeCountSummary(now()))
                .willReturn(asList(
                        new Item(now().plusDays(1), "A", 100, 1),
                        new Item(now().minusDays(1), "B", 200, 9)
                ));
        given(this.zeroRowFiller.fill(any(), any()))
                .willAnswer(invocation -> invocation.getArgument(0)); // return data unchanged

        // when
        List<EnvelopeCountSummary> result = service.getEnvelopeSummaryCountFor(now(), false);

        // then
        assertThat(result)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(
                        new EnvelopeCountSummary(100, 1, "A", now().plusDays(1)),
                        new EnvelopeCountSummary(200, 9, "B", now().minusDays(1))
            );
    }

    @Test
    void envelope_summary_count_report_should_filter_out_test_container_when_requested_for_envelope_count_summary() {
        given(repo.getEnvelopeCountSummary(now()))
                .willReturn(asList(
                        new Item(now(), TEST_CONTAINER, 100, 1),
                        new Item(now(), "some_other_container", 10, 0)
                ));
        given(this.zeroRowFiller.fill(any(), any()))
                .willAnswer(invocation -> invocation.getArgument(0)); // return data unchanged

        // when
        List<EnvelopeCountSummary> resultWithoutTestContainer = service.getEnvelopeSummaryCountFor(now(), false);
        List<EnvelopeCountSummary> resultWithTestContainer = service.getEnvelopeSummaryCountFor(now(), true);

        // then
        assertThat(resultWithoutTestContainer).hasSize(1);
        assertThat(resultWithoutTestContainer.get(0).container).isEqualTo("some_other_container");

        assertThat(resultWithTestContainer).hasSize(2);
    }

    @Test
    void envelope_summary_count_report_should_map_empty_list_from_repo_when_requested_for_envelope_count_summary() {
        given(repo.getEnvelopeCountSummary(now())).willReturn(emptyList());
        given(this.zeroRowFiller.fill(any(), any()))
                .willAnswer(invocation -> invocation.getArgument(0)); // return data unchanged

        // when
        List<EnvelopeCountSummary> result = service.getEnvelopeSummaryCountFor(now(), false);

        // then
        assertThat(result).isEmpty();
    }

    private LocalTime toLocalTime(Instant instant) {
        return LocalTime.parse(DateTimeFormatter.ofPattern("HH:mm:ss").format(instant.atZone(EUROPE_LONDON_ZONE_ID)));
    }
}
