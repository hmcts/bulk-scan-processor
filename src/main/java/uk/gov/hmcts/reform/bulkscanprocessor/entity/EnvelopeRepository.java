package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EnvelopeRepository extends JpaRepository<Envelope, UUID> {

    /**
     * Finds envelopes with  for a given jurisdiction.
     *
     * @param jurisdiction jurisdiction for which envelopes needs to be retrieved
     * @return A list of envelopes which belongs to the given jurisdiction.
     */
    List<Envelope> findByJurisdictionAndStatus(String jurisdiction, Event event);
}
