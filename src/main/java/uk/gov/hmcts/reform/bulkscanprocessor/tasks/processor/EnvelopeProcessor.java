package uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Event;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.MetadataNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.util.EntityParser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;

import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Event.DOC_PROCESSED;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Event.DOC_UPLOADED;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.UPLOAD_FAILURE;

@Component
public class EnvelopeProcessor {
    private static final Logger log = LoggerFactory.getLogger(EnvelopeProcessor.class);

    private final EnvelopeRepository envelopeRepository;
    private final ProcessEventRepository processEventRepository;

    public EnvelopeProcessor(
        EnvelopeRepository envelopeRepository,
        ProcessEventRepository processEventRepository
    ) {
        this.envelopeRepository = envelopeRepository;
        this.processEventRepository = processEventRepository;
    }

    public Envelope parseEnvelope(byte[] metadataStream) throws IOException {
        if (Objects.isNull(metadataStream)) {
            throw new MetadataNotFoundException("No metadata file found in the zip file");
        }
        //TODO Perform json schema validation for the metadata file
        InputStream inputStream = new ByteArrayInputStream(metadataStream);

        return EntityParser.parseEnvelopeMetadata(inputStream);
    }

    public Optional<Envelope> hasEnvelopeFailedToUploadBefore(Envelope envelope) {
        return envelopeRepository.checkLastEnvelopeStatus(
            envelope.getContainer(),
            envelope.getZipFileName(),
            UPLOAD_FAILURE
        );
    }

    public Envelope saveEnvelope(Envelope envelope) {
        Envelope dbEnvelope = envelopeRepository.save(envelope);

        log.info("Envelope for jurisdiction {} and zip file name {} successfully saved in database.",
            envelope.getJurisdiction(),
            envelope.getZipFileName()
        );

        return dbEnvelope;
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
