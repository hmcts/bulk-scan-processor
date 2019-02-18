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
        value = "SELECT env.jurisdiction, env.status, e1.container, "
            + "e1.zipfilename, e1.createdDate, e2.completedDate FROM \n"
            + "(SELECT container, zipfilename, MIN(createdat) AS createdDate "
            + "FROM process_events  \n"
            + "WHERE CAST(createdat AS DATE) = :date\n"
            + "GROUP BY container, zipfilename) e1\n"
            + "LEFT JOIN \n"
            + "(SELECT container, zipfilename, event, MAX(createdat) AS completedDate FROM process_events \n"
            + "GROUP BY container, zipfilename, event) e2 \n"
            + "ON e1.container = e2.container AND e1.zipfilename = e2.zipfilename AND e2.event = 'COMPLETED'\n"
            + "LEFT JOIN envelopes env \n"
            + "ON e1.container = env.container AND e1.zipfilename = env.zipfilename \n"
            + "ORDER BY e1.createdDate ASC"
    )
    List<ZipFileSummaryItem> getZipFileSummaryReportFor(@Param("date") LocalDate date);

}
