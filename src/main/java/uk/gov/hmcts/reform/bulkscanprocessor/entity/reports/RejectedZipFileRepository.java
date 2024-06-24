package uk.gov.hmcts.reform.bulkscanprocessor.entity.reports;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("checkstyle:LineLength")
public interface RejectedZipFileRepository extends JpaRepository<Envelope, UUID> {

    @Query(
            nativeQuery = true,
            value = "SELECT "
                    + "  process_events.container, "
                    + "  process_events.zipfilename, "
                    + "  process_events.event, "
                    + "  MIN(process_events.createdat) AS processingStartedEventDate, "
                    + "  Cast(envelopes.id as varchar) as envelopeId " // null means no envelope
                    // UUID by default it translates to a JDBC Binary type so cast to varchar
                    + "FROM process_events "
                    + "LEFT OUTER JOIN envelopes "
                    + "  ON envelopes.container = process_events.container "
                    + "    AND envelopes.zipfilename = process_events.zipfilename "
                    + "WHERE process_events.event "
                    + "  IN ('DOC_FAILURE', 'FILE_VALIDATION_FAILURE', 'DOC_SIGNATURE_FAILURE') "
                    + "  AND date(process_events.createdat) = :date "
                    + "GROUP BY process_events.container, "
                    + "         process_events.zipfilename, "
                    + "         process_events.event, "
                    + "         envelopeId"
    )
    List<RejectedZipFile> getRejectedZipFilesReportFor(@Param("date") LocalDate date);

    @Query(
        nativeQuery = true,
        value = "SELECT "
            + "  process_events.container, "
            + "  process_events.zipfilename, "
            + "  process_events.event, "
            + "  MIN(process_events.createdat) AS processingStartedEventDate, "
            + "  Cast(envelopes.id as varchar) as envelopeId "
            + "FROM process_events "
            + "LEFT OUTER JOIN envelopes "
            + "  ON envelopes.container = process_events.container "
            + "    AND envelopes.zipfilename = process_events.zipfilename "
            + "WHERE process_events.event "
            + "  IN ('DOC_FAILURE', 'FILE_VALIDATION_FAILURE', 'DOC_SIGNATURE_FAILURE') "
            + "  AND process_events.zipfilename = :name "
            + "GROUP BY process_events.container, "
            + "         process_events.zipfilename, "
            + "         process_events.event, "
            + "         process_events.createdat, "
            + "         envelopeId "
            + "ORDER BY process_events.createdat"
    )
    List<RejectedZipFile> getRejectedZipFilesReportFor(@Param("name") String name);
}
