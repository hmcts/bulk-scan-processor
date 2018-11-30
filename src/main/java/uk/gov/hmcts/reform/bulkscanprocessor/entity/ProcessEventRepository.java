package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessEventRepository extends JpaRepository<ProcessEvent, Long> {
}
