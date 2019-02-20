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
        value = "SELECT createdEvent.container, createdEvent.zipfilename, createdEvent.createdDate, \n"
            + "  lastEventName.event AS status,\n"
            + "  (CASE WHEN lastEventName.event = 'COMPLETED'\n"
            + "        THEN lastEvent.eventDate ELSE null END\n"
            + "  ) AS completedDate\n"
            + "FROM (\n"
            + "  SELECT container, zipfilename, MIN(createdat) AS createdDate FROM process_events \n"
            + "  WHERE date(createdat) = :date AND event='ZIPFILE_PROCESSING_STARTED'\n"
            + "  GROUP BY container, zipfilename\n"
            + ") createdEvent \n" /* zipfile processing started date */
            + "LEFT JOIN (\n"
            + "  SELECT container, zipfilename, MAX(createdat) AS eventDate\n"
            + "  FROM process_events \n"
            + "  GROUP BY container, zipfilename\n"
            + ") lastEvent \n" /* zipfile process completion date */
            + "  ON createdEvent.container = lastEvent.container \n"
            + "  AND createdEvent.zipfilename = lastEvent.zipfilename\n"
            + "LEFT JOIN (\n"
            + "  SELECT container, zipfilename, createdat, event\n"
            + "  FROM process_events\n"
            + ") lastEventName \n" /* latest event of the zipfile */
            + "  ON lastEvent.container = lastEventName.container \n"
            + "  AND lastEvent.zipfilename = lastEventName.zipfilename\n"
            + "  AND lastEvent.eventDate = lastEventName.createdat\n"
            + "ORDER BY createdEvent.createdDate ASC"
    )
    List<ZipFileSummaryItem> getZipFileSummaryReportFor(@Param("date") LocalDate date);

}
