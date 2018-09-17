package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.BlobDeleteFailureException;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.output.Pdf;
import uk.gov.hmcts.reform.bulkscanprocessor.services.wrapper.ErrorHandlingWrapper;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.DocumentProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;

import java.util.List;

import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Event.DOC_PROCESSED;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Event.DOC_UPLOADED;

public abstract class Processor {

    protected final CloudBlobClient cloudBlobClient;
    private final DocumentProcessor documentProcessor;
    protected final EnvelopeProcessor envelopeProcessor;
    protected final ErrorHandlingWrapper errorWrapper;

    protected Processor(
        CloudBlobClient cloudBlobClient,
        DocumentProcessor documentProcessor,
        EnvelopeProcessor envelopeProcessor,
        ErrorHandlingWrapper errorWrapper
    ) {
        this.cloudBlobClient = cloudBlobClient;
        this.documentProcessor = documentProcessor;
        this.envelopeProcessor = envelopeProcessor;
        this.errorWrapper = errorWrapper;
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
        markAsProcessed(envelope);
        deleteBlob(envelope, cloudBlockBlob);
    }

    private Boolean uploadParsedEnvelopeDocuments(
        Envelope envelope,
        List<Pdf> pdfs
    ) {
        return errorWrapper.wrapDocUploadFailure(envelope, () -> {
            documentProcessor.uploadPdfFiles(pdfs, envelope.getScannableItems());
            return Boolean.TRUE;
        });
    }

    private Boolean deleteBlob(
        Envelope envelope,
        CloudBlockBlob cloudBlockBlob
    ) {
        return errorWrapper.wrapDeleteBlobFailure(envelope, () -> {
            // Lease needs to be broken before deleting the blob. 0 implies lease is broken immediately
            cloudBlockBlob.breakLease(0);
            boolean deleted = cloudBlockBlob.deleteIfExists();
            if (!deleted) {
                throw new BlobDeleteFailureException(envelope);
            }
            envelope.setZipDeleted(true);
            envelopeProcessor.saveEnvelope(envelope);
            return Boolean.TRUE;
        });
    }

    private Boolean markAsUploaded(Envelope envelope) {
        return errorWrapper.wrapFailure(() -> {
            envelopeProcessor.handleEvent(envelope, DOC_UPLOADED);
            return Boolean.TRUE;
        });
    }

    public Boolean markAsProcessed(Envelope envelope) {
        return errorWrapper.wrapFailure(() -> {
            envelopeProcessor.handleEvent(envelope, DOC_PROCESSED);
            return Boolean.TRUE;
        });
    }

}
