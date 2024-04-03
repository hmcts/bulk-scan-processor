package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Repository for envelopes.
 */
public interface EnvelopeRepository extends JpaRepository<Envelope, UUID> {

    /**
     * Finds envelopes for a given jurisdiction and status.
     *
     * @param jurisdiction jurisdiction for which envelopes needs to be retrieved
     * @return A list of envelopes which belongs to the given jurisdiction.
     */
    List<Envelope> findByJurisdictionAndStatusAndCreatedAtGreaterThan(
        String jurisdiction,
        Status status,
        Instant time
    );

    int countAllByCreatedAtAfter(Instant createdAt);

    /**
     * Find all envelopes for given jurisdiction.
     * @param jurisdiction jurisdiction
     * @param date date
     * @return list of envelopes
     */
    List<Envelope> findByJurisdictionAndCreatedAtGreaterThan(String jurisdiction, Instant date);

    /**
     * Find by status.
     * @param status status
     * @return list of envelopes
     */
    List<Envelope> findByStatus(Status status);

    /**
     * Find by status in.
     * @param statuses statuses
     * @return list of envelopes
     */
    List<Envelope> findByStatusIn(Collection<Status> statuses);

    /**
     * Find by status and container.
     * @param zipFileName zipFileName
     * @return list of envelopes
     */
    List<Envelope> findByZipFileName(String zipFileName);

    /**
     * Find by ccdId.
     * @param ccdId ccdId
     * @return list of envelopes
     */
    List<Envelope> findByCcdId(String ccdId);

    /**
     * Finds envelope with a blob not deleted for a given container and zip file name.
     *
     * @param container   from where container originated.
     * @param zipFileName of envelope.
     * @return A list of envelopes.
     */
    Envelope findFirstByZipFileNameAndContainerOrderByCreatedAtDesc(
        String zipFileName,
        String container
    );

    /**
     * Finds envelope for a given container, zip file name and status.
     *
     * @param container   from where container originated.
     * @param zipFileName of envelope.
     * @param status      of envelope.
     * @return A list of envelopes.
     */
    @Query("select e from Envelope e"
        + " where e.container = :container"
        + "   and e.zipFileName = :zip"
        + "   and e.status = :status"
        + " order by e.createdAt desc"
    )
    List<Envelope> findRecentEnvelopes(
        @Param("container") String container,
        @Param("zip") String zipFileName,
        @Param("status") Status status,
        Pageable pageable
    );

    /**
     * Finds envelope to upload.
     *
     * @param maxFailureCount max failure count
     * @return A list of envelopes.
     */
    @Query("select e from Envelope e"
        + " where e.status in ('CREATED', 'UPLOAD_FAILURE')" // todo: use a constant
        + "   and e.uploadFailureCount < :maxFailureCount"
        + " order by e.createdAt asc"
    )
    List<Envelope> findEnvelopesToUpload(@Param("maxFailureCount") int maxFailureCount);

    /**
     * Finds number of incomplete envelopes before a given date time.
     *
     * @param dateTime the date time
     * @return A list of envelopes.
     */
    @Query(
        nativeQuery = true,
        value = "SELECT COUNT(1) AS incomplete\n"
            + "FROM envelopes\n"
            + "WHERE createdat < :datetime AND status != 'COMPLETED'"
    )
    int getIncompleteEnvelopesCountBefore(@Param("datetime") LocalDateTime dateTime);

    /**
     * Find list of envelopes by container and status and zipDeleted.
     * @param container container
     * @param status status
     * @param zipDeleted zipDeleted
     * @return list of envelopes
     */
    List<Envelope> findByContainerAndStatusAndZipDeleted(String container, Status status, boolean zipDeleted);

    /**
     * Get incomplete envelopes before a given date time.
     * @param dateTime date time
     * @return list of envelopes
     */
    @Query("select e from Envelope e \n"
        + "WHERE createdat < :datetime AND status != 'COMPLETED' AND status != 'ABORTED'"
    @Query("select e from Envelope e "
        + "where e.createdAt < :datetime AND e.status != 'COMPLETED' AND e.status != 'ABORTED'"
    )
    List<Envelope> getIncompleteEnvelopesBefore(@Param("datetime") Instant dateTime);

    /**
     * Delete envelopes before a given date time.
     * @param dateTime date time
     * @param envelopeIds envelope ids
     * @return number of envelopes deleted
     */
    @Modifying
    @Query(value = "DELETE FROM envelopes e "
        + "WHERE createdat < :datetime AND status != 'COMPLETED' AND status != 'ABORTED' AND e.id IN :envelopeIds",
        nativeQuery = true)
    int deleteEnvelopesBefore(@Param("datetime") LocalDateTime dateTime,
                              @Param("envelopeIds") List<UUID> envelopeIds);

    /**
     * Get complete envelopes from a container.
     * @param container container
     * @return list of envelopes
     */
    @Query("select e from Envelope e \n"
        + "WHERE e.container = :container "
        + "AND (e.status = 'COMPLETED' OR e.status = 'NOTIFICATION_SENT') "
        + "AND e.zipDeleted=false"
    )
    List<Envelope> getCompleteEnvelopesFromContainer(
        @Param("container") String container
    );

    /**
     * Update envelope classification and status.
     * @param id envelope id
     * @param container container
     * @return number of envelopes updated
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Envelope e SET e.classification = 'EXCEPTION', e.status = 'UPLOADED' \n"
        + "WHERE e.id = :id "
        + "AND e.container = :container "
        + "AND e.classification = 'SUPPLEMENTARY_EVIDENCE' "
        + "AND e.status = 'NOTIFICATION_SENT' "
    )
    int updateEnvelopeClassificationAndStatus(
        @Param("id") UUID id,
        @Param("container") String container
    );

}
