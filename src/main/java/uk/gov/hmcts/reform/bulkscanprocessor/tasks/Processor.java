package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.output.Pdf;
import uk.gov.hmcts.reform.bulkscanprocessor.services.wrapper.ErrorHandlingWrapper;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.DocumentProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;

import java.util.List;

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
        errorWrapper.wrapDocUploadFailure(envelope, () -> {
            documentProcessor.processPdfFiles(pdfs, envelope.getScannableItems());
            envelopeProcessor.markAsUploaded(envelope);

            cloudBlockBlob.delete();

            envelopeProcessor.markAsProcessed(envelope);

            return null;
        });
    }
}
