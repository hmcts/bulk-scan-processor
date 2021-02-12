package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;

import java.time.Instant;
import javax.transaction.Transactional;

import static java.util.Collections.emptyList;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.ZIP_PROCESSING_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification.EXCEPTION;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.DOC_FAILURE;

@Component
public class ZipFileFailureHandler {
    private static final Logger log = LoggerFactory.getLogger(ZipFileFailureHandler.class);

    private final EnvelopeProcessor envelopeProcessor;

    private final EnvelopeRepository envelopeRepository;

    public ZipFileFailureHandler(
        EnvelopeProcessor envelopeProcessor,
        EnvelopeRepository envelopeRepository
    ) {
        this.envelopeProcessor = envelopeProcessor;
        this.envelopeRepository = envelopeRepository;
    }

    @Transactional
    public void handleZipFileFailure(String containerName, String zipFileName, Exception ex) {
        log.error("Failed to process file {} from container {}", zipFileName, containerName, ex);
        envelopeProcessor.createEvent(
            DOC_FAILURE,
            containerName,
            zipFileName,
            ex.getMessage(),
            null
        );
        Envelope envelope = createEnvelope(containerName, zipFileName);
        envelope.setStatus(ZIP_PROCESSING_FAILURE);
        envelopeRepository.saveAndFlush(envelope);
    }

    private Envelope createEnvelope(String containerName, String zipFileName) {
        return new Envelope(
            "",
            "",
            Instant.now(),
            Instant.now(),
            Instant.now(),
            zipFileName,
            "",
            null,
            EXCEPTION,
            emptyList(),
            emptyList(),
            emptyList(),
            containerName,
            ""
        );
    }
}
