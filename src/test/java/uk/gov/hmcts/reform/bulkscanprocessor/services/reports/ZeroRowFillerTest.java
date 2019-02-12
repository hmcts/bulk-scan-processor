package uk.gov.hmcts.reform.bulkscanprocessor.services.reports;

import org.junit.Test;

import java.util.List;

import static java.time.LocalDate.now;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class ZeroRowFillerTest {

    @Test
    public void should_add_missing_zero_row_when_needed() {
        // given
        ZeroRowFiller filler = new ZeroRowFiller(new String[] {"A", "B", "C"});

        List<EnvelopeCountSummary> listToFill = asList(
            new EnvelopeCountSummary(100, 101, "A", now()),
            new EnvelopeCountSummary(200, 201, "B", now())
        );

        // when
        List<EnvelopeCountSummary> result = filler.fill(listToFill, now());

        // then
        assertThat(result)
            .usingFieldByFieldElementComparator()
            .containsExactly(
                new EnvelopeCountSummary(100, 101, "A", now()),
                new EnvelopeCountSummary(200, 201, "B", now()),
                new EnvelopeCountSummary(0, 0, "C", now())
            );
    }

    @Test
    public void should_not_change_input_list_if_all_jurisdictions_are_present() {
        // given
        ZeroRowFiller filler = new ZeroRowFiller(new String[] {"A", "B"});

        List<EnvelopeCountSummary> listToFill = asList(
            new EnvelopeCountSummary(100, 101, "A", now()),
            new EnvelopeCountSummary(200, 201, "B", now())
        );

        // when
        List<EnvelopeCountSummary> result = filler.fill(listToFill, now());

        // then
        assertThat(result)
            .usingFieldByFieldElementComparator()
            .containsExactlyElementsOf(listToFill);
    }
}
