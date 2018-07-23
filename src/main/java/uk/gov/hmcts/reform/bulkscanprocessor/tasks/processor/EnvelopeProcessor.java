package uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Event;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.MetadataNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.util.EntityParser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Event.DOC_CONSUMED;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Event.DOC_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Event.DOC_PROCESSED;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Event.DOC_UPLOADED;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Event.DOC_UPLOAD_FAILURE;

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

    public Envelope processEnvelope(byte[] metadataStream, String containerName) throws IOException {
        if (Objects.isNull(metadataStream)) {
            throw new MetadataNotFoundException("No metadata file found in the zip file");
        }
        //TODO Perform json schema validation for the metadata file
        InputStream inputStream = new ByteArrayInputStream(metadataStream);
        Envelope envelope = EntityParser.parseEnvelopeMetadata(inputStream);
        envelope.setContainer(containerName);

        Envelope dbEnvelope = envelopeRepository.save(envelope);

        log.info("Envelope for jurisdiction {} and zip file name {} successfully saved in database.",
            envelope.getJurisdiction(),
            envelope.getZipFileName()
        );

        return dbEnvelope;
    }

    public void markAsUploaded(Envelope envelope, String containerName, String zipFileName) {
        persistEvent(null, envelope, containerName, zipFileName, DOC_UPLOADED);
    }

    public void markAsUploadFailed(String reason, Envelope envelope, String containerName, String zipFileName) {
        persistEvent(reason, envelope, containerName, zipFileName, DOC_UPLOAD_FAILURE);
    }

    public void markAsGenericFailure(String reason, Envelope envelope, String containerName, String zipFileName) {
        persistEvent(reason, envelope, containerName, zipFileName, DOC_FAILURE);
    }

    public void markAsProcessed(Envelope envelope, String containerName, String zipFileName) {
        persistEvent(null, envelope, containerName, zipFileName, DOC_PROCESSED);
    }

    public void markAsConsumed(Envelope envelope) {
        // TODO Need to pass container name once it is available in Envelope
        persistEvent(null, envelope, envelope.getJurisdiction(), envelope.getZipFileName(), DOC_CONSUMED);
    }

    private void persistEvent(
        String reason,
        Envelope envelope,
        String containerName,
        String zipFileName,
        Event event
    ) {
        ProcessEvent processEvent = new ProcessEvent(containerName, zipFileName, event);

        processEvent.setReason(reason);
        processEventRepository.save(processEvent);

        envelope.setStatus(event);
        envelopeRepository.save(envelope);
    }
}
