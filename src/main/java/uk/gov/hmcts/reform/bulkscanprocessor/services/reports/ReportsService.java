package uk.gov.hmcts.reform.bulkscanprocessor.services.reports;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.EnvelopeCountSummaryItem;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.EnvelopeCountSummaryRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.ZipFileSummary;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.ZipFilesSummaryRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.EnvelopeCountSummary;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.ZipFileSummaryResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.utils.ZeroRowFiller;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static uk.gov.hmcts.reform.bulkscanprocessor.util.TimeZones.EUROPE_LONDON_ZONE_ID;

/**
 * Service to handle reports.
 */
@Service
public class ReportsService {

    private static final Logger log = LoggerFactory.getLogger(ReportsService.class);

    public static final String TEST_CONTAINER = "bulkscan";

    private final EnvelopeCountSummaryRepository repo;
    private final ZeroRowFiller zeroRowFiller;

    private final ZipFilesSummaryRepository zipFilesSummaryRepository;

    /**
     * Constructor for the ReportsService.
     */
    public ReportsService(
        EnvelopeCountSummaryRepository repo,
        ZeroRowFiller zeroRowFiller,
        ZipFilesSummaryRepository zipFilesSummaryRepository
    ) {
        this.repo = repo;
        this.zeroRowFiller = zeroRowFiller;
        this.zipFilesSummaryRepository = zipFilesSummaryRepository;
    }

    /**
     * Get count summary for the given date.
     * @param date date to get the count summary
     * @param includeTestContainer whether to include test container in the report
     * @return list of count summary
     */
    public List<EnvelopeCountSummary> getCountFor(LocalDate date, boolean includeTestContainer) {
        long start = System.currentTimeMillis();
        List<EnvelopeCountSummaryItem> countSummaryItems = repo.getReportFor(date);
        final List<EnvelopeCountSummary> reportResult = getEnvelopeCountSummaries(
                date,
                includeTestContainer,
                countSummaryItems
        );
        log.info("Count summary report took {} ms", System.currentTimeMillis() - start);
        return reportResult;
    }

    /**
     * Get count summary for the given date.
     * @param date date to get the count summary
     * @param includeTestContainer whether to include test container in the report
     * @return list of count summary
     */
    public List<EnvelopeCountSummary> getSummaryCountFor(LocalDate date, boolean includeTestContainer) {
        long start = System.currentTimeMillis();
        List<EnvelopeCountSummaryItem> countSummaryItems = repo.getSummaryReportFor(date);
        final List<EnvelopeCountSummary> reportResult = getEnvelopeCountSummaries(
                date,
                includeTestContainer,
                countSummaryItems
        );
        log.info("Count summary report took {} ms", System.currentTimeMillis() - start);
        return reportResult;
    }

    /**
     * Get zip files summary for the given date and container.
     * @param date      zip file received date
     * @param container to filter the zip files when container value is provided
     * @return list of zip files summary
     */
    public List<ZipFileSummaryResponse> getZipFilesSummary(
        LocalDate date,
        String container,
        Classification classification
    ) {
        Predicate<ZipFileSummaryResponse> predicate =
            summary -> isEmpty(container) || summary.container.equalsIgnoreCase(container);
        if (classification != null) {
            predicate = predicate.and(summary -> classification.name().equalsIgnoreCase(summary.classification));
        }
        return zipFilesSummaryRepository.getZipFileSummaryReportFor(date)
                .stream()
                .map(this::fromDbZipfileSummary)
                .filter(predicate)
                .collect(toList());
    }

    /**
     * Get envelope summary count for the given date.
     * @param date date to get the envelope summary count
     * @param includeTestContainer whether to include test container in the report
     * @return list of envelope summary count
     */
    public List<EnvelopeCountSummary> getEnvelopeSummaryCountFor(LocalDate date, boolean includeTestContainer) {
        long start = System.currentTimeMillis();
        List<EnvelopeCountSummaryItem> countSummaryItems = repo.getEnvelopeCountSummary(date);
        final List<EnvelopeCountSummary> reportResult = getEnvelopeCountSummaries(
                date,
                includeTestContainer,
                countSummaryItems
        );
        log.info("Envelope count summary report took {} ms", System.currentTimeMillis() - start);
        return reportResult;
    }

    /**
     * Get envelope summary count for the given date.
     * @param date date to get the envelope summary count
     * @param includeTestContainer whether to include test container in the report
     * @return list of envelope summary count
     */
    private List<EnvelopeCountSummary> getEnvelopeCountSummaries(
            LocalDate date,
            boolean includeTestContainer,
            List<EnvelopeCountSummaryItem> countSummaryItems
    ) {
        return zeroRowFiller
                .fill(countSummaryItems.stream().map(this::fromDb).collect(toList()), date)
                .stream()
                .filter(it -> includeTestContainer || !Objects.equals(it.container, TEST_CONTAINER))
                .collect(toList());
    }

    /**
     * Convert db item to response.
     * @param dbItem db item
     * @return response
     */
    private ZipFileSummaryResponse fromDbZipfileSummary(ZipFileSummary dbItem) {
        return new ZipFileSummaryResponse(
            dbItem.getZipFileName(),
            toLocalDate(dbItem.getCreatedDate()),
            toLocalTime(dbItem.getCreatedDate()),
            toLocalDate(dbItem.getCompletedDate()),
            toLocalTime(dbItem.getCompletedDate()),
            dbItem.getContainer(),
            dbItem.getLastEventStatus(),
            dbItem.getEnvelopeStatus(),
            dbItem.getClassification(),
            dbItem.getCcdId(),
            dbItem.getCcdAction()
        );
    }

    /**
     * Convert db item to response.
     * @param dbItem db item
     * @return response
     */
    private EnvelopeCountSummary fromDb(EnvelopeCountSummaryItem dbItem) {
        return new EnvelopeCountSummary(
            dbItem.getReceived(),
            dbItem.getRejected(),
            dbItem.getContainer(),
            dbItem.getDate()
        );
    }

    /**
     * Convert Instant to LocalDate.
     * @param instant instant
     * @return LocalDate
     */
    private LocalDate toLocalDate(Instant instant) {
        if (instant != null) {
            return LocalDateTime.ofInstant(instant, EUROPE_LONDON_ZONE_ID).toLocalDate();
        }
        return null;
    }

    /**
     * Convert Instant to LocalTime.
     * @param instant instant
     * @return LocalTime
     */
    private LocalTime toLocalTime(Instant instant) {
        if (instant != null) {
            return LocalTime.parse(DateTimeFormatter.ofPattern("HH:mm:ss")
                .format(instant.atZone(EUROPE_LONDON_ZONE_ID)));
        }
        return null;
    }
}
