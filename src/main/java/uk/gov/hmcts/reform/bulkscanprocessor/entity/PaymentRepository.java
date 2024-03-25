package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for payments.
 */
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    /**
     * Updates the status of payments with the given document control numbers.
     * @param dcns document control numbers
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Payment p SET p.status = 'SUBMITTED'"
        + "WHERE p.documentControlNumber in :dcn")
    int updateStatus(@Param("dcn") List<String> dcns);

    /**
     * Finds payments by document control number.
     * @param paymentDcns document control numbers
     * @return payments
     */
    Optional<List<Payment>> findByDocumentControlNumberIn(List<String> paymentDcns);
}
