package uk.gov.hmcts.reform.bulkscanprocessor.services.reports;

import org.apache.commons.lang3.NotImplementedException;
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

    public List<ZipFileSummary> getZipFilesSummary(LocalDate date, String jurisdiction) {
        throw new NotImplementedException("Not yet implemented");
    }

    EnvelopeCountSummary fromDb(EnvelopeCountSummaryItem dbItem) {
        return new EnvelopeCountSummary(
            dbItem.getReceived(),
            dbItem.getRejected(),
            toJurisdiction(dbItem.getContainer()),
            dbItem.getDate()
        );
    }

    private String toJurisdiction(String container) {
        // this is the current implicit convention. It may require more 'sophisticated' mapping in the future...
        return container.toUpperCase();
    }
}
