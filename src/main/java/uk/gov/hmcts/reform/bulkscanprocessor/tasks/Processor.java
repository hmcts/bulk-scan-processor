package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.microsoft.azure.storage.blob.CloudBlockBlob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Event;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.output.Pdf;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.BlobManager;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.DocumentProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;

import java.util.List;

import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Event.DOC_PROCESSED;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Event.DOC_UPLOADED;


public abstract class Processor {

    private static final Logger log = LoggerFactory.getLogger(Processor.class);

    protected final BlobManager blobManager;
    private final DocumentProcessor documentProcessor;
    protected final EnvelopeProcessor envelopeProcessor;
    protected final EnvelopeRepository envelopeRepository;
    protected final ProcessEventRepository eventRepository;

    @Value("${storage.signature_algorithm}")
    protected String signatureAlg;

    @Value("${storage.public_key_der_file}")
    protected String publicKeyDerFilename;

    protected Processor(
        BlobManager blobManager,
        DocumentProcessor documentProcessor,
        EnvelopeProcessor envelopeProcessor,
        EnvelopeRepository envelopeRepository,
        ProcessEventRepository eventRepository
    ) {
        this.blobManager = blobManager;
        this.documentProcessor = documentProcessor;
        this.envelopeProcessor = envelopeProcessor;
        this.envelopeRepository = envelopeRepository;
        this.eventRepository = eventRepository;
    }

    protected void processParsedEnvelopeDocuments(
        Envelope envelope,
        List<Pdf> pdfs,
        CloudBlockBlob cloudBlockBlob
    ) {
        if (!uploadParsedEnvelopeDocuments(envelope, pdfs)) {
            return;
        }
        if (!markAsUploaded(envelope)) {
            return;
        }
        if (!markAsProcessed(envelope)) {
            return;
        }

        deleteBlob(envelope, cloudBlockBlob);
    }

    protected void handleEventRelatedError(Event event, String containerName, String zipFilename, Exception exception) {
        registerEvent(event, containerName, zipFilename, exception.getMessage());
        log.error(exception.getMessage(), exception);
    }

    protected void registerEvent(Event event, String container, String zipFileName, String reason) {
        ProcessEvent processEvent = new ProcessEvent(
            container,
            zipFileName,
            event
        );

        processEvent.setReason(reason);
        eventRepository.save(processEvent);

        log.info(
            "Zip {} from {} marked as {}",
            processEvent.getZipFileName(),
            processEvent.getContainer(),
            processEvent.getEvent()
        );
    }

    private Boolean uploadParsedEnvelopeDocuments(
        Envelope envelope,
        List<Pdf> pdfs
    ) {
        try {
            documentProcessor.uploadPdfFiles(pdfs, envelope.getScannableItems());
            return Boolean.TRUE;
        } catch (Exception ex) {
            incrementUploadFailureCount(envelope);
            updateEnvelopeLastStatus(envelope, Event.DOC_UPLOAD_FAILURE);
            handleEventRelatedError(Event.DOC_UPLOAD_FAILURE, envelope.getContainer(), envelope.getZipFileName(), ex);
            return Boolean.FALSE;
        }
    }

    private void incrementUploadFailureCount(Envelope envelope) {
        envelope.setUploadFailureCount(envelope.getUploadFailureCount() + 1);
        envelopeRepository.save(envelope);
    }

    private Boolean deleteBlob(
        Envelope envelope,
        CloudBlockBlob cloudBlockBlob
    ) {
        try {
            // Lease needs to be broken before deleting the blob. 0 implies lease is broken immediately
            cloudBlockBlob.breakLease(0);
            boolean deleted = cloudBlockBlob.deleteIfExists();
            if (!deleted) {
                handleBlobDeletionError(envelope, null);
            }
            envelope.setZipDeleted(true);
            envelopeProcessor.saveEnvelope(envelope);
            return Boolean.TRUE;
        } catch (Exception ex) {
            handleBlobDeletionError(envelope, ex);
            return Boolean.FALSE;
        }
    }

    private void handleBlobDeletionError(Envelope envelope, Exception cause) {
        handleEventRelatedError(
            Event.BLOB_DELETE_FAILURE,
            envelope.getContainer(),
            envelope.getZipFileName(),
            cause
        );
    }

    private Boolean markAsUploaded(Envelope envelope) {
        return handleEvent(envelope, DOC_UPLOADED);
    }

    private Boolean markAsProcessed(Envelope envelope) {
        return handleEvent(envelope, DOC_PROCESSED);
    }

    private Boolean handleEvent(Envelope envelope, Event event) {
        try {
            envelopeProcessor.handleEvent(envelope, event);
            return Boolean.TRUE;
        } catch (Exception exception) {
            log.error(exception.getMessage(), exception);
            return Boolean.FALSE;
        }
    }

    private void updateEnvelopeLastStatus(Envelope envelope, Event event) {
        Status.fromEvent(event).ifPresent(status -> {
            envelope.setStatus(status);

            envelopeRepository.save(envelope);

            log.info(
                "Change envelope {} from {} and {} status to {}",
                envelope.getId(),
                envelope.getContainer(),
                envelope.getZipFileName(),
                status
            );
        });
    }
}
