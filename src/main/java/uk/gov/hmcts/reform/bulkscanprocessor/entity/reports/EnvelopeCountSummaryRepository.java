package uk.gov.hmcts.reform.bulkscanprocessor.entity.reports;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("checkstyle:LineLength")
public interface EnvelopeCountSummaryRepository extends JpaRepository<Envelope, UUID> {

    @Query(
        nativeQuery = true,
        value = "SELECT\n"
            + "  first_events.container AS container,\n"
            + "  first_events.date AS date,\n"
            + "  count(*) AS received,\n"
            + "  SUM(CASE WHEN rejection_events.id IS NOT NULL THEN 1 ELSE 0 END) AS rejected\n"
            + "FROM (\n"
            + "  SELECT DISTINCT on (container, zipfilename)\n"
            + "    container, zipfilename, date(createdat)\n"
            + "  FROM process_events\n"
            + "  WHERE date(createdat) >= :earliest AND date(createdat) <= :date\n"
            + "  ORDER BY container, zipfilename, date(createdat) ASC\n"
            + ") AS first_events\n"
            + "LEFT JOIN (\n"
            + "  SELECT DISTINCT on (container, zipfilename)\n"
            + "    1 AS id, container, zipfilename\n"
            + "  FROM process_events\n"
            + "  WHERE event IN ('DOC_FAILURE', 'FILE_VALIDATION_FAILURE', 'DOC_SIGNATURE_FAILURE')\n"
            + ") AS rejection_events\n"
            + "ON rejection_events.container = first_events.container\n"
            + "AND rejection_events.zipfilename = first_events.zipfilename\n"
            + "GROUP BY first_events.container, first_events.date\n"
            + "HAVING date = :date\n"
    )
    List<EnvelopeCountSummaryItem> getReportFor(
        @Param("earliest") LocalDate earliest,
        @Param("date") LocalDate date
    );
}
