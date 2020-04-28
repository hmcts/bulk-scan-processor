package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.output.Pdf;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.BlobManager;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.DocumentProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;

import java.util.List;

import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.DOC_PROCESSED;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.DOC_UPLOADED;


public abstract class Processor {

    private static final Logger log = LoggerFactory.getLogger(Processor.class);

    protected final BlobManager blobManager;
    private final DocumentProcessor documentProcessor;
    protected final EnvelopeProcessor envelopeProcessor;
    protected final EnvelopeRepository envelopeRepository;
    protected final ProcessEventRepository eventRepository;

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
        List<Pdf> pdfs
    ) {
        if (!uploadParsedEnvelopeDocuments(envelope, pdfs)) {
            return;
        }
        if (!markAsUploaded(envelope)) {
            return;
        }

        markAsProcessed(envelope);
    }

    // prepare to move to envelope processor
    protected void handleEventRelatedError(
        Event event,
        String containerName,
        String zipFilename,
        String reason,
        Envelope envelope
    ) {
        envelopeProcessor.createEvent(
            event,
            containerName,
            zipFilename,
            reason,
            envelope == null ? null : envelope.getId()
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
            log.error(
                "Failed to upload PDF files to Document Management (zip {}, container {})",
                envelope.getZipFileName(),
                envelope.getContainer(),
                ex
            );

            envelopeProcessor.markAsUploadFailure(envelope);
            envelopeProcessor.createEvent(
                Event.DOC_UPLOAD_FAILURE,
                envelope.getContainer(),
                envelope.getZipFileName(),
                ex.getMessage(),
                envelope.getId()
            );
            return Boolean.FALSE;
        }
    }

    private Boolean markAsUploaded(Envelope envelope) {
        return handleEvent(envelope, DOC_UPLOADED);
    }

    private void markAsProcessed(Envelope envelope) {
        handleEvent(envelope, DOC_PROCESSED);
    }

    private Boolean handleEvent(Envelope envelope, Event event) {
        try {
            envelopeProcessor.handleEvent(envelope, event);
            return Boolean.TRUE;
        } catch (Exception exception) {
            log.error(
                "Failed to update database for the event: {} zip file: {} jurisdiction: {}",
                event.name(),
                envelope.getZipFileName(),
                envelope.getJurisdiction(),
                exception
            );
            return Boolean.FALSE;
        }
    }
}
