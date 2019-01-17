package uk.gov.hmcts.reform.bulkscanprocessor.services.reports;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.EnvelopeCountSummaryItem;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.EnvelopeCountSummaryRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.toList;

@Service
public class ReportsService {

    public static final String TEST_JURISDICTION = "BULKSCAN";

    private final EnvelopeCountSummaryRepository repo;
    private final ZeroRowFiller zeroRowFiller;

    // region constructor
    public ReportsService(
        EnvelopeCountSummaryRepository repo,
        ZeroRowFiller zeroRowFiller
    ) {
        this.repo = repo;
        this.zeroRowFiller = zeroRowFiller;
    }
    // endregion

    public List<EnvelopeCountSummary> getCountFor(LocalDate date, boolean includeTestJurisdiction) {
        return zeroRowFiller
            .fill(repo.getReportFor(date).stream().map(this::fromDb).collect(toList()), date)
            .stream()
            .filter(it -> includeTestJurisdiction || !Objects.equals(it.jurisdiction, TEST_JURISDICTION))
            .collect(toList());
    }

    EnvelopeCountSummary fromDb(EnvelopeCountSummaryItem dbItem) {
        return new EnvelopeCountSummary(
            dbItem.getReceived(),
            dbItem.getRejected(),
            dbItem.getJurisdiction(),
            dbItem.getDate()
        );
    }
}
