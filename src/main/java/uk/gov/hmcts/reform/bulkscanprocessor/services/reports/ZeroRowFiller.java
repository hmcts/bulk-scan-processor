package uk.gov.hmcts.reform.bulkscanprocessor.services.reports;

import com.google.common.collect.Sets;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

@Component
public class ZeroRowFiller {

    private final Set<String> jurisdictions;

    public ZeroRowFiller(@Qualifier("jurisdictions") Set<String> jurisdictions) {
        this.jurisdictions = jurisdictions;
    }

    public List<EnvelopeCountSummary> fill(List<EnvelopeCountSummary> listToFill, LocalDate date) {
        return Stream.concat(
            listToFill.stream(),
            missingJurisdictions(listToFill).stream().map(jur -> new EnvelopeCountSummary(0, 0, jur, date))
        ).collect(toList());
    }

    private Set<String> missingJurisdictions(List<EnvelopeCountSummary> listToFill) {
        return Sets.difference(
            this.jurisdictions,
            listToFill.stream().map(res -> res.jurisdiction).collect(toSet())
        );
    }
}
