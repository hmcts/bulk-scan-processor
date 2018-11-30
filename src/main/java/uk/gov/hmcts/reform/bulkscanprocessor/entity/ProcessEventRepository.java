package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.model.db.ProcessEvent;

public interface ProcessEventRepository extends JpaRepository<ProcessEvent, Long> {
}
