package uk.gov.hmcts.reform.bulkscanprocessor.services.reports;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.EnvelopeCountSummaryRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.ZipFilesSummaryRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.helper.reports.countsummary.Item;
import uk.gov.hmcts.reform.bulkscanprocessor.helper.reports.zipfilesummary.ZipFileSummaryItem;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.EnvelopeCountSummary;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.ZipFileSummaryResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.utils.ZeroRowFiller;

import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static java.time.LocalDate.now;
import static java.time.LocalDateTime.ofInstant;
import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.COMPLETED;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.ZIPFILE_PROCESSING_STARTED;
import static uk.gov.hmcts.reform.bulkscanprocessor.services.reports.ReportsService.TEST_CONTAINER;

@RunWith(MockitoJUnitRunner.class)
public class ReportsServiceTest {

    @Mock private EnvelopeCountSummaryRepository repo;
    @Mock private ZeroRowFiller zeroRowFiller;
    @Mock private ZipFilesSummaryRepository zipFilesSummaryRepo;

    private ReportsService service;

    @Before
    public void setUp() throws Exception {
        this.service = new ReportsService(this.repo, zeroRowFiller, zipFilesSummaryRepo);
        when(this.zeroRowFiller.fill(any(), any()))
            .thenAnswer(invocation -> invocation.getArgument(0)); // return data unchanged
    }

    @Test
    public void should_map_repo_result_properly_when_requested_for_envelope_count_summary() {
        given(repo.getReportFor(now()))
            .willReturn(asList(
                new Item(now().plusDays(1), "A", 100, 1),
                new Item(now().minusDays(1), "B", 200, 9)
            ));

        // when
        List<EnvelopeCountSummary> result = service.getCountFor(now(), false);

        // then
        assertThat(result)
            .usingFieldByFieldElementComparator()
            .containsExactly(
                new EnvelopeCountSummary(100, 1, "A", now().plusDays(1)),
                new EnvelopeCountSummary(200, 9, "B", now().minusDays(1))
            );
    }

    @Test
    public void should_filter_out_test_container_when_requested_for_envelope_count_summary() {
        given(repo.getReportFor(now()))
            .willReturn(asList(
                new Item(now(), TEST_CONTAINER, 100, 1),
                new Item(now(), "some_other_container", 10, 0)
            ));

        // when
        List<EnvelopeCountSummary> resultWithoutTestContainer = service.getCountFor(now(), false);
        List<EnvelopeCountSummary> resultWithTestContainer = service.getCountFor(now(), true);

        // then
        assertThat(resultWithoutTestContainer).hasSize(1);
        assertThat(resultWithoutTestContainer.get(0).container).isEqualTo("some_other_container");

        assertThat(resultWithTestContainer).hasSize(2);
    }

    @Test
    public void should_map_empty_list_from_repo_when_requested_for_envelope_count_summary() {
        given(repo.getReportFor(now()))
            .willReturn(emptyList());

        // when
        List<EnvelopeCountSummary> result = service.getCountFor(now(), false);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    public void should_map_empty_list_from_repo_when_requested_for_zipfiles_summary() {
        given(zipFilesSummaryRepo.getZipFileSummaryReportFor(now()))
            .willReturn(emptyList());

        // when
        List<ZipFileSummaryResponse> result = service.getZipFilesSummary(now(), null);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    public void should_map_db_results_when_requested_for_zipfiles_summary() {
        Instant instant = Instant.now();
        given(zipFilesSummaryRepo.getZipFileSummaryReportFor(now()))
            .willReturn(asList(
                new ZipFileSummaryItem(
                    "t1.zip", instant.minus(1, MINUTES), null, "c1", ZIPFILE_PROCESSING_STARTED.toString()
                ),
                new ZipFileSummaryItem(
                    "t2.zip", instant.minus(10, MINUTES), instant.minus(20, MINUTES), "c2", COMPLETED.toString()
                )
            ));

        // when
        List<ZipFileSummaryResponse> result = service.getZipFilesSummary(now(), null);

        // then
        assertThat(result)
            .usingFieldByFieldElementComparator()
            .containsExactly(
                new ZipFileSummaryResponse(
                    "t1.zip",
                    ofInstant(instant.minus(1, MINUTES), UTC).toLocalDate(),
                    toLocalTime(instant.minus(1, MINUTES)),
                    null,
                    null,
                    "c1",
                    ZIPFILE_PROCESSING_STARTED.toString()
                ),
                new ZipFileSummaryResponse(
                    "t2.zip",
                    ofInstant(instant.minus(10, MINUTES), UTC).toLocalDate(),
                    toLocalTime(instant.minus(10, MINUTES)),
                    ofInstant(instant.minus(20, MINUTES), UTC).toLocalDate(),
                    toLocalTime(instant.minus(20, MINUTES)),
                    "c2",
                    COMPLETED.toString()
                )
            );
    }

    @Test
    public void should_filter_zipfiles_by_container_when_requested_for_zipfiles_summary() {
        Instant instant = Instant.now();
        given(zipFilesSummaryRepo.getZipFileSummaryReportFor(now()))
            .willReturn(asList(
                new ZipFileSummaryItem(
                    "t1.zip", instant.minus(1, MINUTES), null, "c1", ZIPFILE_PROCESSING_STARTED.toString()
                ),
                new ZipFileSummaryItem(
                    "t2.zip", instant.minus(10, MINUTES), instant.minus(20, MINUTES), "c2", COMPLETED.toString()
                )
            ));

        // when
        List<ZipFileSummaryResponse> result = service.getZipFilesSummary(now(), "c2");

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).container).isEqualTo("c2");

        assertThat(result.get(0).fileName).isEqualTo("t2.zip");
    }

    private LocalTime toLocalTime(Instant instant) {
        return LocalTime.parse(DateTimeFormatter.ofPattern("HH:mm:ss").format(instant.atZone(UTC)));
    }

}
