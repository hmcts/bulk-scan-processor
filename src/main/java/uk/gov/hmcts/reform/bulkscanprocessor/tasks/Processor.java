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

    protected CloudBlobClient cloudBlobClient;
    protected DocumentProcessor documentProcessor;
    protected EnvelopeProcessor envelopeProcessor;
    protected ErrorHandlingWrapper errorWrapper;

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
