package uk.gov.hmcts.reform.bulkscanprocessor.services.reports;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.bulkscanprocessor.config.ContainerMappings;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.EnvelopeCountSummary;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.utils.ZeroRowFiller;

import java.util.List;

import static java.time.LocalDate.now;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class ZeroRowFillerTest {

    @Test
    public void should_add_missing_zero_row_when_needed() {
        // given
        ContainerMappings containerMappings = new ContainerMappings();
        containerMappings.setMappings(asList(
            new ContainerMappings.Mapping("c1", "j1", "123", "https://example.com/s1"),
            new ContainerMappings.Mapping("c2", "j2", "124", "https://example.com/s2"),
            new ContainerMappings.Mapping("c3", "j3", "125", "https://example.com/s3")
        ));

        ZeroRowFiller filler = new ZeroRowFiller(containerMappings);

        List<EnvelopeCountSummary> listToFill = asList(
            new EnvelopeCountSummary(100, 101, "c1", now()),
            new EnvelopeCountSummary(200, 201, "c2", now())
        );

        // when
        List<EnvelopeCountSummary> result = filler.fill(listToFill, now());

        // then
        assertThat(result)
            .usingFieldByFieldElementComparator()
            .containsExactly(
                new EnvelopeCountSummary(100, 101, "c1", now()),
                new EnvelopeCountSummary(200, 201, "c2", now()),
                new EnvelopeCountSummary(0, 0, "c3", now())
            );
    }

    @Test
    public void should_not_change_input_list_if_all_jurisdictions_are_present() {
        // given
        ContainerMappings containerMappings = new ContainerMappings();
        containerMappings.setMappings(asList(
            new ContainerMappings.Mapping("c1", "j1", "123", "https://example.com/s1"),
            new ContainerMappings.Mapping("c2", "j2", "124", "https://example.com/s1")
        ));

        ZeroRowFiller filler = new ZeroRowFiller(containerMappings);

        List<EnvelopeCountSummary> listToFill = asList(
            new EnvelopeCountSummary(100, 101, "c1", now()),
            new EnvelopeCountSummary(200, 201, "c2", now())
        );

        // when
        List<EnvelopeCountSummary> result = filler.fill(listToFill, now());

        // then
        assertThat(result)
            .usingFieldByFieldElementComparator()
            .containsExactlyElementsOf(listToFill);
    }
}
