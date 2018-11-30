package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.model.db.DbScannableItem;

import java.util.UUID;

public interface ScannableItemRepository extends JpaRepository<DbScannableItem, UUID> {
}
