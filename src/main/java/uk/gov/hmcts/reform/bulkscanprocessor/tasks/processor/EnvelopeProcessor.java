package uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Event;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.MetadataNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.PreviouslyFailedToUploadException;
import uk.gov.hmcts.reform.bulkscanprocessor.validation.MetafileJsonValidator;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Event.DOC_PROCESSED;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Event.DOC_UPLOADED;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.UPLOAD_FAILURE;

@Component
public class EnvelopeProcessor {
    private static final Logger log = LoggerFactory.getLogger(EnvelopeProcessor.class);

    private final MetafileJsonValidator schemaValidator;
    private final EnvelopeRepository envelopeRepository;
    private final ProcessEventRepository processEventRepository;
    private final int reUploadBatchSize;
    private final int maxReuploadTriesCount;

    public EnvelopeProcessor(
        MetafileJsonValidator schemaValidator,
        EnvelopeRepository envelopeRepository,
        ProcessEventRepository processEventRepository,
        @Value("${scheduling.task.reupload.batch}") int reUploadBatchSize,
        @Value("${scheduling.task.reupload.max_tries}") int maxReuploadTriesCount
    ) {
        this.schemaValidator = schemaValidator;
        this.envelopeRepository = envelopeRepository;
        this.processEventRepository = processEventRepository;
        this.reUploadBatchSize = reUploadBatchSize;
        this.maxReuploadTriesCount = maxReuploadTriesCount;
    }

    public Envelope parseEnvelope(byte[] metadataStream) throws IOException, ProcessingException {
        if (Objects.isNull(metadataStream)) {
            throw new MetadataNotFoundException("No metadata file found in the zip file");
        }

        schemaValidator.validate(metadataStream);

        return schemaValidator.parseMetafile(metadataStream);
    }

    /**
     * Assert envelope did not fail to be uploaded in the past.
     * Throws exception otherwise.
     *
     * @param envelope details to check against.
     */
    public void assertDidNotFailToUploadBefore(Envelope envelope) {
        List<Envelope> envelopes = envelopeRepository.findRecentEnvelopes(
            envelope.getContainer(),
            envelope.getZipFileName(),
            UPLOAD_FAILURE,
            PageRequest.of(0, 1)
        );

        if (envelopes.size() == 1) {
            Envelope failedEnvelope = envelopes.get(0);

            throw new PreviouslyFailedToUploadException(
                failedEnvelope.getContainer(),
                failedEnvelope.getZipFileName(),
                String.format(
                    "Envelope %s created at %s is already marked as failed to upload. Skipping",
                    failedEnvelope.getId(),
                    failedEnvelope.getCreatedAt()
                )
            );
        }
    }

    public Envelope saveEnvelope(Envelope envelope) {
        Envelope dbEnvelope = envelopeRepository.save(envelope);

        log.info("Envelope for jurisdiction {} and zip file name {} successfully saved in database.",
            envelope.getJurisdiction(),
            envelope.getZipFileName()
        );

        return dbEnvelope;
    }

    public List<Envelope> getFailedToUploadEnvelopes(String jurisdiction) {
        return envelopeRepository.findEnvelopesToResend(
            jurisdiction,
            maxReuploadTriesCount,
            reUploadBatchSize > 0 ? PageRequest.of(0, reUploadBatchSize) : null
        );
    }

    public void markAsUploaded(Envelope envelope) {
        persistEvent(envelope, envelope.getContainer(), envelope.getZipFileName(), DOC_UPLOADED);
    }

    public void markAsProcessed(Envelope envelope) {
        persistEvent(envelope, envelope.getContainer(), envelope.getZipFileName(), DOC_PROCESSED);
    }

    private void persistEvent(
        Envelope envelope,
        String containerName,
        String zipFileName,
        Event event
    ) {
        processEventRepository.save(new ProcessEvent(containerName, zipFileName, event));

        if (envelope != null) {
            Status.fromEvent(event).ifPresent(status -> {
                envelope.setStatus(status);
                envelopeRepository.save(envelope);
            });
        }
    }
}
