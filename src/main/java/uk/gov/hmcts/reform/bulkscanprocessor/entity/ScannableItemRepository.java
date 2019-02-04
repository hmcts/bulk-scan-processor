package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ScannableItemRepository extends JpaRepository<ScannableItem, UUID> {

}
