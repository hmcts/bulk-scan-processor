package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EnvelopeRepository extends JpaRepository<Envelope, UUID> {
}
