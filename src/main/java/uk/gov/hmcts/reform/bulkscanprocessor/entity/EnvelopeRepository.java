package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import org.springframework.data.jpa.repository.JpaRepository;

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
     * Finds envelopes for a given container and zip file name.
     *
     * @param container from where container originated
     * @param zipFileName of envelope
     * @return A list of envelopes
     */
    List<Envelope> findByContainerAndZipFileName(String container, String zipFileName);
}
