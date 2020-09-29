package uk.gov.hmcts.reform.bulkscanprocessor.entity.reports;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ReceivedZipFileRepository extends JpaRepository<Envelope, UUID> {

    @Query(
        nativeQuery = true,
        value = "SELECT "
            + "  process_events.container, "
            + "  process_events.zipfilename, "
            + "  process_events.createdat AS processingStartedEventDate, "
            + "  scannable_items.documentControlNumber AS scannableItemDCN, "
            + "  payments.documentControlNumber AS paymentDCN, "
            + "  envelopes.rescanFor, "
            + "  Cast(envelopes.id as varchar) as envelopeId " // null means no envelope
            + "FROM process_events "
            + "LEFT OUTER JOIN envelopes "
            + "  ON envelopes.container = process_events.container "
            + "    AND envelopes.zipfilename = process_events.zipfilename "
            + "LEFT OUTER JOIN scannable_items "
            + "  ON envelopes.id = scannable_items.envelope_id "
            + "LEFT OUTER JOIN payments "
            + "  ON envelopes.id = payments.envelope_id "
            + "WHERE process_events.event = 'ZIPFILE_PROCESSING_STARTED'"
            + "  AND date(process_events.createdat) = :date"
    )
    List<ReceivedZipFile> getReceivedZipFilesReportFor(@Param("date") LocalDate date);
}
