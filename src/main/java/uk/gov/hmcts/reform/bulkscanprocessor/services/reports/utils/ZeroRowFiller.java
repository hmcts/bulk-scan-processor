package uk.gov.hmcts.reform.bulkscanprocessor.services.reports.utils;

import com.google.common.collect.Sets;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.config.ContainerMappings;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.EnvelopeCountSummary;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * Fills the missing containers with zero count.
 */
@Component
@EnableConfigurationProperties(ContainerMappings.class)
public class ZeroRowFiller {

    private final List<String> containers;

    /**
     * Constructor for the ZeroRowFiller.
     * @param containerMappings The container mappings
     */
    public ZeroRowFiller(ContainerMappings containerMappings) {
        this.containers = containerMappings.getMappings()
            .stream()
            .map(ContainerMappings.Mapping::getContainer).collect(toList());
    }

    /**
     * Fills the missing containers with zero count.
     * @param listToFill The list to fill
     * @param date The date
     * @return The list with missing containers filled with zero count
     */
    public List<EnvelopeCountSummary> fill(List<EnvelopeCountSummary> listToFill, LocalDate date) {
        return Stream.concat(
            listToFill.stream(),
            missingContainers(listToFill).stream().map(container -> new EnvelopeCountSummary(0, 0, container, date))
        ).collect(toList());
    }

    /**
     * Returns the missing containers.
     * @param listToFill The list to fill
     * @return The missing containers
     */
    private Set<String> missingContainers(List<EnvelopeCountSummary> listToFill) {
        return Sets.difference(
            Sets.newHashSet(this.containers),
            listToFill.stream().map(res -> res.container).collect(toSet())
        );
    }
}
