package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProcessEventRepository extends JpaRepository<ProcessEvent, Long> {
    List<ProcessEvent> findByZipFileName(String zipFileName);
}
