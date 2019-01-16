package uk.gov.hmcts.reform.bulkscanprocessor.services.reports;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.EnvelopeCountSummaryRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.helper.reports.countsummary.Item;

import java.util.List;

import static java.time.LocalDate.now;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.bulkscanprocessor.services.reports.ReportsService.TEST_JURISDICTION;

@RunWith(MockitoJUnitRunner.class)
public class ReportsServiceTest {

    @Mock private EnvelopeCountSummaryRepository repo;
    @Mock private ZeroRowFiller zeroRowFiller;

    private ReportsService service;

    @Before
    public void setUp() throws Exception {
        this.service = new ReportsService(this.repo, zeroRowFiller);
        when(this.zeroRowFiller.fill(any(), any()))
            .thenAnswer(invocation -> invocation.getArgument(0)); // return data unchanged
    }

    @Test
    public void should_map_repo_result_properly() {
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
    public void should_filter_out_test_jurisdiction_when_requested() {
        given(repo.getReportFor(now()))
            .willReturn(asList(
                new Item(now(), TEST_JURISDICTION, 100, 1),
                new Item(now(), "SOME_OTHER_JURISDICTION", 10, 0)
            ));

        // when
        List<EnvelopeCountSummary> resultWithoutTestJurisdiction = service.getCountFor(now(), false);
        List<EnvelopeCountSummary> resultWithTestJurisdiction = service.getCountFor(now(), true);

        // then
        assertThat(resultWithoutTestJurisdiction).hasSize(1);
        assertThat(resultWithoutTestJurisdiction.get(0).jurisdiction).isEqualTo("SOME_OTHER_JURISDICTION");

        assertThat(resultWithTestJurisdiction).hasSize(2);
    }

    @Test
    public void should_map_empty_list_from_repo() {
        given(repo.getReportFor(now()))
            .willReturn(emptyList());

        // when
        List<EnvelopeCountSummary> result = service.getCountFor(now(), false);

        // then
        assertThat(result).isEmpty();
    }
}
