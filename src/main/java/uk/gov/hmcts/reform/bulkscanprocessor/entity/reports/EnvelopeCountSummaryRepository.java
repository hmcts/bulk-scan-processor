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
            + "  container as jurisdiction,\n"
            + "  date,\n"
            + "  count(*) AS received,\n"
            + "  SUM(CASE WHEN event IN ('DOC_FAILURE', 'FILE_VALIDATION_FAILURE', 'DOC_SIGNATURE_FAILURE') THEN 1 ELSE 0 END) AS rejected\n"
            + "FROM (\n"
            + "  SELECT DISTINCT on (container, zipfilename)\n"
            + "  container, zipfilename, date(createdat), event\n"
            + "  FROM process_events\n"
            + "  ORDER BY container, zipfilename, date(createdat) ASC\n"
            + ") AS first_events\n"
            + "GROUP BY container, date\n"
            + "HAVING date = :date"
    )
    List<EnvelopeCountSummaryItem> getReportFor(@Param("date") LocalDate date);
}
