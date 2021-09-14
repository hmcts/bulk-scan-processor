package uk.gov.hmcts.reform.bulkscanprocessor.entity.reports;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("checkstyle:LineLength")
public interface ReceivedScannableItemRepository extends JpaRepository<Envelope, UUID> {

    @Query(
            nativeQuery = true,
            value = "SELECT COUNT(*) AS COUNT, e.container "
                    + "FROM scannable_items s "
                    + "LEFT JOIN envelopes e "
                    + "ON s.envelope_id=e.id "
                    + "WHERE date(s.scanningdate) = :date "
                    + "GROUP BY e.container"
    )
    List<ReceivedScannableItem> getReceivedScannableItemsFor(@Param("date") LocalDate date);
}
