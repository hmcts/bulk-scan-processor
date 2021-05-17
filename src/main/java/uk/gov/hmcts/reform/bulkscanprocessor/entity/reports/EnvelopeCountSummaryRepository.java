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
            + "  date(first_events.createdat) AS date,\n"
            + "  count(*) AS received,\n"
            + "  SUM(CASE WHEN rejection_events.id IS NOT NULL THEN 1 ELSE 0 END) AS rejected\n"
            + "FROM (\n"
            + "  SELECT DISTINCT on (container, zipfilename)\n"
            + "    container, zipfilename, createdat\n"
            + "  FROM process_events\n"
            + "  ORDER BY container, zipfilename, createdat ASC\n"
            + ") AS first_events\n"
            + "LEFT JOIN (\n"
            + "  SELECT DISTINCT on (container, zipfilename)\n"
            + "    id, container, zipfilename, event\n"
            + "  FROM ("
            + "    SELECT DISTINCT on (container, zipfilename)\n"
            + "      id, container, zipfilename, event\n"
            + "    FROM process_events\n"
            + "    ORDER BY container, zipfilename, createdat DESC\n"
            + "  ) AS last_events\n"
            + "  WHERE event IN ('DOC_FAILURE', 'FILE_VALIDATION_FAILURE', 'DOC_SIGNATURE_FAILURE')\n"
            + ") AS rejection_events\n"
            + "ON rejection_events.container = first_events.container\n"
            + "AND rejection_events.zipfilename = first_events.zipfilename\n"
            + "GROUP BY first_events.container, date(first_events.createdat)\n"
            + "HAVING date(first_events.createdat) = :date\n"
    )
    List<EnvelopeCountSummaryItem> getReportFor(@Param("date") LocalDate date);
}
