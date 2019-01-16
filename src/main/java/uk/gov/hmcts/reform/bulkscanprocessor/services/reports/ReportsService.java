package uk.gov.hmcts.reform.bulkscanprocessor.services.reports;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.EnvelopeCountSummaryRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.toList;

@Service
public class ReportsService {

    public static final String TEST_JURISDICTION = "BULKSCAN";

    private final EnvelopeCountSummaryRepository repo;

    public ReportsService(EnvelopeCountSummaryRepository repo) {
        this.repo = repo;
    }

    public List<EnvelopeCountSummary> getCountFor(LocalDate date, boolean includeTestJurisdiction) {
        return repo
            .getReportFor(date)
            .stream()
            .filter(it -> includeTestJurisdiction || !Objects.equals(it.getJurisdiction(), TEST_JURISDICTION))
            .map(it -> new EnvelopeCountSummary(
                it.getReceived(),
                it.getRejected(),
                it.getJurisdiction(),
                it.getDate()
            ))
            .collect(toList());
    }
}
