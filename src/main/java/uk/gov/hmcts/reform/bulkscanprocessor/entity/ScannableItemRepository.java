package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ScannableItemRepository extends JpaRepository<ScannableItem, UUID> {
    @Modifying(clearAutomatically = true)
    @Query("update ScannableItem set ocrData = null, ocrDataJson = null where envelope_id = :envelopeId")
    int clearOcrData(@Param("envelopeId") UUID envelopeId);
}
