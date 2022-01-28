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
            + "    id, container, zipfilename\n"
            + "  FROM process_events\n"
            + "  WHERE event IN ('DOC_FAILURE', 'FILE_VALIDATION_FAILURE', 'DOC_SIGNATURE_FAILURE')\n"
            + ") AS rejection_events\n"
            + "ON rejection_events.container = first_events.container\n"
            + "AND rejection_events.zipfilename = first_events.zipfilename\n"
            + "GROUP BY first_events.container, date(first_events.createdat)\n"
            + "HAVING date(first_events.createdat) = :date\n"
    )
    List<EnvelopeCountSummaryItem> getReportFor(@Param("date") LocalDate date);

    @Query(
        nativeQuery = true,
        value = "SELECT\n"
            + "  first_events.container AS container,\n"
            + "  date(:date) AS date,\n"
            + "  count(*) AS received,\n"
            + "  SUM(CASE WHEN rejection_events.id IS NOT NULL THEN 1 ELSE 0 END) AS rejected\n"
            + "FROM (\n"
            + "  SELECT DISTINCT on (container, zipfilename)\n"
            + "    container, zipfilename, createdat\n"
            + "  FROM process_events\n"
            + "  WHERE date(createdat) = :date"
            + "  ORDER BY container, zipfilename, createdat ASC\n"
            + ") AS first_events\n"
            + "LEFT JOIN (\n"
            + "  SELECT id, container, zipfilename\n"
            + "  FROM ("
            + "    SELECT DISTINCT on (container, zipfilename)\n"
            + "      id, container, zipfilename, event, createdat\n"
            + "    FROM process_events\n"
            + "    ORDER BY container, zipfilename, createdat DESC\n"
            + "  ) AS latest_events\n"
            + "  WHERE event IN ('DOC_FAILURE', 'FILE_VALIDATION_FAILURE', 'DOC_SIGNATURE_FAILURE')\n"
            + "  AND date(createdat) = :date"
            + ") AS rejection_events\n"
            + "ON rejection_events.container = first_events.container\n"
            + "AND rejection_events.zipfilename = first_events.zipfilename\n"
            + "GROUP BY first_events.container\n"
    )
    List<EnvelopeCountSummaryItem> getSummaryReportFor(@Param("date") LocalDate date);

    @Query(
        nativeQuery = true,
        value = "SELECT \n"
            + "  all_data.container AS container, \n"
            + "  all_data.date AS date, \n"
            + "  SUM(all_data.received) AS received,\n"
            + "  SUM(all_data.rejected) AS rejected\n"
            + "FROM \n"
            + "  (\n"
            + "    SELECT\n"
            + "      env.container,\n"
            + "      date(:date) AS date,\n"
            + "      count(env.*) AS received,\n"
            + "      SUM(CASE WHEN env.status IN ('METADATA_FAILURE', 'UPLOAD_FAILURE') THEN 1 ELSE 0 END) AS rejected\n"
            + "    FROM envelopes env\n"
            + "    WHERE date(env.zipfilecreateddate)=date(:date)\n"
            + "    GROUP BY container\n"
            + "  UNION\n"
            + "    SELECT\n"
            + "      ev.container,\n"
            + "      date(:date) AS date,\n"
            + "      count(ev.*) AS received,\n"
            + "      count(ev.*) AS rejected\n"
            + "    FROM process_events ev\n"
            + "    WHERE ev.event='FILE_VALIDATION_FAILURE' AND date(ev.createdat)=date(:date)\n"
            + "    GROUP BY container\n"
            + "  ) AS all_data\n"
            + "GROUP BY all_data.container, all_data.date\n"
    )
    List<EnvelopeCountSummaryItem> getEnvelopeCountSummary(@Param("date") LocalDate date);
}
