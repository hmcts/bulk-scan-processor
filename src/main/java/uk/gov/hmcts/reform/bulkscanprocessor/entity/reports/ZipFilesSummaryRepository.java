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
        value = "SELECT e.zipfilename, e.createdat createdDate, \n"
            + "e.status, e.jurisdiction, MIN(p.createdat) completedDate \n"
            + "FROM envelopes e \n"
            + "LEFT JOIN process_events p ON (e.zipfilename = p.zipfilename\n"
            + "and e.container = p.container)\n"
            + "  AND p.event = 'COMPLETED'\n"
            + "WHERE CAST(e.createdat AS DATE) = :date \n"
            + "GROUP BY e.zipfilename, e.createdat,  \n"
            + "e.status, e.jurisdiction"
    )
    List<ZipFileSummaryItem> getZipFileSummaryReportFor(@Param("date") LocalDate date);

}
