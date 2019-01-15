package uk.gov.hmcts.reform.bulkscanprocessor.services.reports;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.EnvelopeCountSummaryRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.helper.reports.countsummary.Item;

import java.util.List;

import static java.time.LocalDate.now;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@RunWith(MockitoJUnitRunner.class)
public class ReportsServiceTest {

    @Mock
    EnvelopeCountSummaryRepository repo;

    @Test
    public void should_map_repo_result_properly() {
        given(repo.getReportFor(now()))
            .willReturn(asList(
                new Item(now(), "A", 100, 1),
                new Item(now(), "B", 200, 9)
            ));

        ReportsService service = new ReportsService(repo);

        // when
        List<EnvelopeCountSummary> result = service.getCountFor(now());

        // then
        assertThat(result)
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new EnvelopeCountSummary(100, 1, "A", now()),
                new EnvelopeCountSummary(200, 9, "B", now())
            );
    }
}
