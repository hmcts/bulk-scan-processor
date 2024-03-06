package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeJdbcRepository;

import java.util.UUID;
import jakarta.transaction.Transactional;

@Service
public class EnvelopeMarkAsDeletedService {
    private static final Logger log = LoggerFactory.getLogger(EnvelopeMarkAsDeletedService.class);

    private final EnvelopeJdbcRepository envelopeJdbcRepository;

    public EnvelopeMarkAsDeletedService(EnvelopeJdbcRepository envelopeJdbcRepository) {
        this.envelopeJdbcRepository = envelopeJdbcRepository;
    }

    @Transactional
    public void markEnvelopeAsDeleted(UUID envelopeId, String loggingContext) {
        envelopeJdbcRepository.markEnvelopeAsDeleted(envelopeId);
        log.info("Marked envelope as deleted. {}", loggingContext);
    }
}
