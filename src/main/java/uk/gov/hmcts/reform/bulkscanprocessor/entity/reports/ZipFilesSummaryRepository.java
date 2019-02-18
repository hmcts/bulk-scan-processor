package uk.gov.hmcts.reform.bulkscanprocessor.entity.reports;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ZipFilesSummaryRepository extends JpaRepository<Envelope, UUID> {

    @Query(
        nativeQuery = true,
        value = "SELECT env.jurisdiction, env.status, e1.container, e1.zipfilename, \n"
            + "MIN(e1.createdat) AS createdDate, MAX(e2.createdat) AS completedDate \n"
            + "FROM process_events e1 \n"
            + " LEFT JOIN process_events e2 ON e2.zipfilename = e1.zipfilename \n"
            + "  AND e2.container = e2.container\n"
            + "  AND e2.event = 'COMPLETED'\n"
            + " LEFT JOIN envelopes env ON env.container = e1.container\n"
            + "  AND env.zipfilename = e1.zipfilename\n"
            + " WHERE CAST(e1.createdat AS DATE) = '2019-02-15'\n"
            + "GROUP BY env.jurisdiction, env.status, e1.container, e1.zipfilename\n"
            + "ORDER BY createdDate ASC"
    )
    List<ZipFileSummaryItem> getZipFileSummaryReportFor(@Param("date") LocalDate date);

}
