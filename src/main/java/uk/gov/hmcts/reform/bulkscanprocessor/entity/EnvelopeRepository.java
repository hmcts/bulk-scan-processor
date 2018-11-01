package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface EnvelopeRepository extends JpaRepository<Envelope, UUID> {

    /**
     * Finds envelopes for a given jurisdiction and status.
     *
     * @param jurisdiction jurisdiction for which envelopes needs to be retrieved
     * @return A list of envelopes which belongs to the given jurisdiction.
     */
    List<Envelope> findByJurisdictionAndStatus(String jurisdiction, Status status);

    /**
     * Find all envelopes for given jurisdiction.
     */
    List<Envelope> findByJurisdiction(String jurisdiction);

    List<Envelope> findByStatus(Status status);

    /**
     * Finds envelope with a blob not deleted for a given container and zip file name.
     *
     * @param container from where container originated.
     * @param zipFileName of envelope.
     * @return A list of envelopes.
     */
    @Query("select e from Envelope e"
        + " where e.container = :container"
        + "   and e.zipFileName = :zip"
        + " order by e.createdAt desc"
    )
    List<Envelope> findEnvelopesByFileAndContainer(
        @Param("container") String container,
        @Param("zip") String zipFileName,
        Pageable pageable
    );

    /**
     * Finds envelope for a given container, zip file name and status.
     *
     * @param container from where container originated.
     * @param zipFileName of envelope.
     * @param status of envelope.
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
     * Finds first N envelopes for a given jurisdiction that should be resent.
     *
     * @param jurisdiction to filter upon
     * @param pageable limit of data to be processed by consumer
     * @return A list of envelopes
     */
    @Query("select e from Envelope e"
        + " where e.jurisdiction = :jurisdiction"
        + "   and e.status = 'UPLOAD_FAILURE'" // todo: use a constant
        + "   and e.uploadFailureCount < :maxFailureCount"
        + " order by e.createdAt asc"
    )
    List<Envelope> findEnvelopesToResend(
        @Param("jurisdiction") String jurisdiction,
        @Param("maxFailureCount") int maxFailureCount,
        Pageable pageable
    );
}
