package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Payment p SET p.status = 'REQUESTED'"
        + "WHERE p.documentControlNumber in :dcn")
    int updateStatus(@Param("dcn") List<String> dcns);

    Optional<List<Payment>> findByDocumentControlNumberIn(List<String> paymentDcns);
}
