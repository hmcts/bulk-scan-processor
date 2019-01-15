package uk.gov.hmcts.reform.bulkscanprocessor.entity.reports;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface EnvelopeCountSummaryRepository extends JpaRepository<Envelope, UUID> {

    @Query(
        nativeQuery = true,
        value = "SELECT "
            + "  date(createdat) AS date, "
            + "  jurisdiction, "
            + "  count(*) AS received, "
            + "  SUM(CASE WHEN status in ('METADATA_FAILURE', 'SIGNATURE_FAILURE') THEN 1 ELSE 0 END) AS rejected "
            + "FROM envelopes "
            + "GROUP BY date(createdat), jurisdiction "
            + "HAVING date(createdat) = :date"
    )
    List<EnvelopeCountSummaryItem> getReportFor(@Param("date") LocalDate date);
}
