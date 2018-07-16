package uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeState;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeStateRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.MetadataNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.util.EntityParser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import static uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeStatus.DOC_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeStatus.DOC_UPLOADED;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeStatus.DOC_UPLOAD_FAILURE;

@Component
public class EnvelopeProcessor {
    private static final Logger log = LoggerFactory.getLogger(EnvelopeProcessor.class);

    private final EnvelopeRepository envelopeRepository;
    private final EnvelopeStateRepository envelopeStateRepository;

    public EnvelopeProcessor(
        EnvelopeRepository envelopeRepository,
        EnvelopeStateRepository envelopeStateRepository
    ) {
        this.envelopeRepository = envelopeRepository;
        this.envelopeStateRepository = envelopeStateRepository;
    }

    public Envelope processEnvelope(byte[] metadataStream) throws IOException {
        if (Objects.isNull(metadataStream)) {
            throw new MetadataNotFoundException("No metadata file found in the zip file");
        }
        //TODO Perform json schema validation for the metadata file
        InputStream inputStream = new ByteArrayInputStream(metadataStream);
        Envelope envelope = EntityParser.parseEnvelopeMetadata(inputStream);

        Envelope dbEnvelope = envelopeRepository.save(envelope);

        log.info("Envelope for jurisdiction {} and zip file name {} successfully saved in database.",
            envelope.getJurisdiction(),
            envelope.getZipFileName()
        );

        return dbEnvelope;
    }

    public void markAsUploaded(Envelope envelope, String containerName, String zipFileName) {
        EnvelopeState state = new EnvelopeState(containerName, zipFileName);

        state.setEnvelope(envelope);
        state.setStatus(DOC_UPLOADED);

        envelopeStateRepository.save(state);
    }

    public void markAsUploadFailed(String reason, Envelope envelope, String container, String zipFileName) {
        EnvelopeState state = new EnvelopeState(container, zipFileName);

        state.setEnvelope(envelope);
        state.setStatus(DOC_UPLOAD_FAILURE);
        state.setReason(reason);

        envelopeStateRepository.save(state);
    }

    public void markAsGenericFailure(String reason, Envelope envelope, String container, String zipFileName) {
        EnvelopeState state = new EnvelopeState(container, zipFileName);

        state.setEnvelope(envelope);
        state.setStatus(DOC_FAILURE);
        state.setReason(reason);

        envelopeStateRepository.save(state);
    }
}
