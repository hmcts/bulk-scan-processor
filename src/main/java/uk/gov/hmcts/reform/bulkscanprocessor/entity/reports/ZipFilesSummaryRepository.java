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
        value = "SELECT createdEvent.container, createdEvent.zipfilename, createdEvent.createdDate, "
            + "  lastEventName.event AS lastEventStatus, envelope.status AS envelopeStatus, "
            + "  (CASE WHEN lastEventName.event = 'COMPLETED' "
            + "        THEN lastEvent.eventDate ELSE null END "
            + "  ) AS completedDate "
            + "FROM ( "
            + "  SELECT container, zipfilename, MIN(createdat) AS createdDate FROM process_events "
            + "  WHERE date(createdat) = :date AND event='ZIPFILE_PROCESSING_STARTED' "
            + "  GROUP BY container, zipfilename "
            + ") createdEvent " /* zipfile processing started date */
            + "JOIN ( "
            + "  SELECT container, zipfilename, MAX(createdat) AS eventDate "
            + "  FROM process_events "
            + "  GROUP BY container, zipfilename "
            + ") lastEvent " /* zipfile process completion date */
            + "  ON createdEvent.container = lastEvent.container "
            + "  AND createdEvent.zipfilename = lastEvent.zipfilename "
            + "JOIN ( "
            + "  SELECT container, zipfilename, createdat, event "
            + "  FROM process_events "
            + ") lastEventName " /* latest event of the zipfile */
            + "  ON lastEvent.container = lastEventName.container "
            + "  AND lastEvent.zipfilename = lastEventName.zipfilename "
            + "  AND lastEvent.eventDate = lastEventName.createdat "
            + "LEFT JOIN envelopes AS envelope "
            + "  ON createdEvent.zipfilename = envelope.zipfilename "
            + "  AND createdEvent.container = envelope.container "
            + "ORDER BY createdEvent.createdDate ASC"
    )
    List<ZipFileSummary> getZipFileSummaryReportFor(@Param("date") LocalDate date);

}
