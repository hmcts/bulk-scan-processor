package uk.gov.hmcts.reform.bulkscanprocessor.entity.reports;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ReceivedPaymentRepository extends JpaRepository<Envelope, UUID> {

    @Query(
            nativeQuery = true,
            value = "SELECT COUNT(*), e.container "
                    + "FROM payments p "
                    + "LEFT JOIN envelopes e "
                    + "ON p.envelope_id=e.id "
                    + "WHERE date(e.deliveryDate) = :date "
                    + "GROUP BY e.container;"
    )
    List<ReceivedPayment> getReceivedPaymentsFor(@Param("date") LocalDate date);
}
