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
     * Finds envelope for a given container, zip file name and status.
     * Providing {@code PageRequest.of(0, 1)} should allow to return {@code Optional<Envelope>}
     * but that is not the case even after following this notion in documentation.
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
    List<Envelope> checkLastEnvelopeByStatus(
        @Param("container") String container,
        @Param("zip") String zipFileName,
        @Param("status") Status status,
        Pageable pageable
    );
}
