package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
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
        uploadParsedEnvelopeDocuments(envelope, pdfs);
        markAsUploaded(envelope);

        deleteBlob(envelope, cloudBlockBlob);
        markAsProcessed(envelope);
    }

    private void uploadParsedEnvelopeDocuments(
        Envelope envelope,
        List<Pdf> pdfs
    ) {
        errorWrapper.wrapDocUploadFailure(envelope, () -> {
            documentProcessor.uploadPdfFiles(pdfs, envelope.getScannableItems());
            return null;
        });
        markAsUploaded(envelope);
    }

    private void deleteBlob(
        Envelope envelope,
        CloudBlockBlob cloudBlockBlob
    ) {
        errorWrapper.wrapDeleteBlobFailure(envelope, () -> {
            // Lease needs to be broken before deleting the blob. 0 implies lease is broken immediately
            cloudBlockBlob.breakLease(0);
            cloudBlockBlob.deleteIfExists();
            return null;
        });
    }

    private void markAsUploaded(Envelope envelope) {
        errorWrapper.wrapFailure(() -> {
            envelopeProcessor.handleEvent(envelope, DOC_UPLOADED);
            return null;
        });
    }

    public void markAsProcessed(Envelope envelope) {
        errorWrapper.wrapFailure(() -> {
            envelopeProcessor.handleEvent(envelope, DOC_PROCESSED);
            return null;
        });
    }

}
