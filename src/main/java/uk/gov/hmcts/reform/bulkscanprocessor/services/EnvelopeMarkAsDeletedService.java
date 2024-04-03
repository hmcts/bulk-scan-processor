package uk.gov.hmcts.reform.bulkscanprocessor.services;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeJdbcRepository;

import java.util.UUID;

/**
 * Service to mark an envelope as deleted.
 */
@Service
public class EnvelopeMarkAsDeletedService {
    private static final Logger log = LoggerFactory.getLogger(EnvelopeMarkAsDeletedService.class);

    private final EnvelopeJdbcRepository envelopeJdbcRepository;

    /**
     * Constructor for the EnvelopeMarkAsDeletedService.
     * @param envelopeJdbcRepository The envelope JDBC repository
     */
    public EnvelopeMarkAsDeletedService(EnvelopeJdbcRepository envelopeJdbcRepository) {
        this.envelopeJdbcRepository = envelopeJdbcRepository;
    }

    /**
     * Marks an envelope as deleted.
     * @param envelopeId The envelope ID
     * @param loggingContext The logging context
     */
    @Transactional
    public void markEnvelopeAsDeleted(UUID envelopeId, String loggingContext) {
        envelopeJdbcRepository.markEnvelopeAsDeleted(envelopeId);
        log.info("Marked envelope as deleted. {}", loggingContext);
    }
}
