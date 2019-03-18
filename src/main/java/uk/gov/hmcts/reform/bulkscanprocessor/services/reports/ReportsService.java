package uk.gov.hmcts.reform.bulkscanprocessor.services.reports;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.EnvelopeCountSummaryItem;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.EnvelopeCountSummaryRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.ZipFileSummary;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.ZipFilesSummaryRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.time.LocalDateTime.ofInstant;
import static java.time.ZoneOffset.UTC;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isEmpty;

@Service
public class ReportsService {

    public static final String TEST_CONTAINER = "bulkscan";

    private final EnvelopeCountSummaryRepository repo;
    private final ZeroRowFiller zeroRowFiller;

    private final ZipFilesSummaryRepository zipFilesSummaryRepository;

    // region constructor
    public ReportsService(
        EnvelopeCountSummaryRepository repo,
        ZeroRowFiller zeroRowFiller,
        ZipFilesSummaryRepository zipFilesSummaryRepository
    ) {
        this.repo = repo;
        this.zeroRowFiller = zeroRowFiller;
        this.zipFilesSummaryRepository = zipFilesSummaryRepository;
    }
    // endregion

    public List<EnvelopeCountSummary> getCountFor(LocalDate date, boolean includeTestContainer) {
        return zeroRowFiller
            .fill(repo.getReportFor(date).stream().map(this::fromDb).collect(toList()), date)
            .stream()
            .filter(it -> includeTestContainer || !Objects.equals(it.container, TEST_CONTAINER))
            .collect(toList());
    }

    /**
     * Get zip files summary for the given date and container.
     *
     * @param date      zip file received date
     * @param container to filter the zip files when container value is provided
     * @return list of zip files summary
     */
    public List<ZipFileSummaryResponse> getZipFilesSummary(LocalDate date, String container) {
        return zipFilesSummaryRepository.getZipFileSummaryReportFor(date)
            .stream()
            .map(this::fromDbZipfileSummary)
            .filter(summary -> isEmpty(container) || summary.container.equalsIgnoreCase(container))
            .collect(Collectors.toList());
    }

    private ZipFileSummaryResponse fromDbZipfileSummary(ZipFileSummary dbItem) {
        return new ZipFileSummaryResponse(
            dbItem.getZipFileName(),
            ofInstant(dbItem.getCreatedDate(), UTC).toLocalDate(),
            toLocalTime(dbItem.getCreatedDate()),
            toLocalDate(dbItem.getCompletedDate()),
            toLocalTime(dbItem.getCompletedDate()),
            dbItem.getContainer(),
            dbItem.getStatus()
        );
    }

    private EnvelopeCountSummary fromDb(EnvelopeCountSummaryItem dbItem) {
        return new EnvelopeCountSummary(
            dbItem.getReceived(),
            dbItem.getRejected(),
            dbItem.getContainer(),
            dbItem.getDate()
        );
    }

    private LocalDate toLocalDate(Instant instant) {
        if (instant != null) {
            return LocalDateTime.ofInstant(instant, UTC).toLocalDate();
        }
        return null;
    }

    private LocalTime toLocalTime(Instant instant) {
        if (instant != null) {
            return LocalTime.parse(DateTimeFormatter.ofPattern("HH:mm:ss").format(instant.atZone(UTC)));
        }
        return null;
    }
}
