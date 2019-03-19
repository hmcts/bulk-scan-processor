package uk.gov.hmcts.reform.bulkscanprocessor.services.reports;

import com.google.common.collect.Sets;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.config.ContainerMappings;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

@Component
@EnableConfigurationProperties(ContainerMappings.class)
public class ZeroRowFiller {

    private final List<String> containers;

    public ZeroRowFiller(ContainerMappings containerMappings) {
        this.containers = containerMappings.getMappings()
            .stream()
            .map(ContainerMappings.Mapping::getContainer).collect(toList());
    }

    public List<EnvelopeCountSummary> fill(List<EnvelopeCountSummary> listToFill, LocalDate date) {
        return Stream.concat(
            listToFill.stream(),
            missingContainers(listToFill).stream().map(container -> new EnvelopeCountSummary(0, 0, container, date))
        ).collect(toList());
    }

    private Set<String> missingContainers(List<EnvelopeCountSummary> listToFill) {
        return Sets.difference(
            Sets.newHashSet(this.containers),
            listToFill.stream().map(res -> res.container).collect(toSet())
        );
    }
}
