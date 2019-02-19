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
        value = "SELECT e1.container, e1.zipfilename, e1.createdDate, e3.event, \n"
            + "  (CASE WHEN e3.event = 'COMPLETED' THEN e2.lastEventDate ELSE null END) AS completedDate FROM \n"
            + "  (SELECT container, zipfilename, MIN(createdat) AS createdDate FROM process_events  \n"
            + "    WHERE date(createdat) = :date AND event='ZIPFILE_PROCESSING_STARTED'\n"
            + "    GROUP BY container, zipfilename) e1\n"
            + "  LEFT JOIN \n"
            + "  (SELECT container, zipfilename, max(createdat) AS lastEventDate FROM process_events \n"
            + "   GROUP BY container, zipfilename) e2 \n"
            + "  ON e1.container = e2.container \n"
            + "  AND e1.zipfilename = e2.zipfilename\n"
            + "  LEFT JOIN \n"
            + "  (SELECT container, zipfilename, createdat, event FROM process_events) e3 \n"
            + "  ON e2.container = e3.container \n"
            + "  AND e2.zipfilename = e3.zipfilename\n"
            + "  AND e2.lastEventDate = e3.createdat\n"
            + "ORDER BY e1.createdDate ASC\n"
    )
    List<ZipFileSummaryItem> getZipFileSummaryReportFor(@Param("date") LocalDate date);

}
