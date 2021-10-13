package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ProcessEventRepository extends JpaRepository<ProcessEvent, Long> {
    List<ProcessEvent> findByZipFileName(String zipFileName);

    @Query(
            nativeQuery = true,
            value = "SELECT * "
                    + "FROM process_events "
                    + "WHERE zipFileName LIKE :dcnPrefix"
                    + "% "
                    + "AND DATE(createdAt) BETWEEN :fromDate AND :toDate "
                    + "ORDER BY zipFileName, createdAt DESC"
    )
    List<ProcessEvent> findEventsByDcnPrefix(
            @Param("dcnPrefix") String dcnPrefix,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );
}
