package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ProcessEventRepository extends JpaRepository<ProcessEvent, Long> {
    List<ProcessEvent> findByZipFileName(String zipFileName);

    @Query("select ev from ProcessEvent ev"
        + " where ev.createdAt = :date"
        + "   and ev.event IN ('DOC_FAILURE', 'FILE_VALIDATION_FAILURE', 'DOC_SIGNATURE_FAILURE')"
        + " order by ev.date desc"
    )
    List<ProcessEvent> getRejectionEvents(@Param("date") Instant date);
}
