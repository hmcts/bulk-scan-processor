package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for process events.
 */
public interface ProcessEventRepository extends JpaRepository<ProcessEvent, Long> {

    /**
     * Returns the latest events for the given zip file name.
     * @param zipFileName name of the zip file
     * @return list of process events
     */
    List<ProcessEvent> findByZipFileNameOrderByCreatedAtDesc(String zipFileName);

    /**
     * Find events by dcn prefix and date range.
     * @param dcnPrefix prefix of the dcn
     * @param fromDate from date
     * @param toDate to date
     * @return list of process events
     */
    @Query(
            nativeQuery = true,
            value = "SELECT id, container, zipFileName, createdAt, event, reason "
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
