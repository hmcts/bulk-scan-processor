package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Event;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.BlobDeleteFailureException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.EnvelopeMsg;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.output.Pdf;
import uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.ServiceBusHelper;
import uk.gov.hmcts.reform.bulkscanprocessor.services.wrapper.ErrorHandlingWrapper;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.DocumentProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;

import java.util.List;

import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Event.DOC_PROCESSED;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Event.DOC_PROCESSED_NOTIFICATION_SENT;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Event.DOC_UPLOADED;

public abstract class Processor {

    protected final CloudBlobClient cloudBlobClient;
    private final DocumentProcessor documentProcessor;
    protected final EnvelopeProcessor envelopeProcessor;
    protected final ErrorHandlingWrapper errorWrapper;

    @Value("${storage.signature_algorithm}")
    protected String signatureAlg;

    @Value("${storage.public_key_der_file}")
    protected String publicKeyDerFilename;


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

    protected abstract ServiceBusHelper serviceBusHelper();

    protected void processParsedEnvelopeDocuments(
        Envelope envelope,
        List<Pdf> pdfs,
        CloudBlockBlob cloudBlockBlob,
        ServiceBusHelper serviceBusHelper
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
        if (!sendProcessedMessage(serviceBusHelper, envelope)) {
            return;
        }
        markAsNotified(envelope);
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

    private Boolean sendProcessedMessage(ServiceBusHelper serviceBusHelper, Envelope envelope) {
        return errorWrapper.wrapNotificationFailure(envelope, () -> {
            serviceBusHelper.sendMessage(new EnvelopeMsg(envelope));
            return Boolean.TRUE;
        });
    }

    private Boolean markAsUploaded(Envelope envelope) {
        return handleEvent(envelope, DOC_UPLOADED);
    }

    private Boolean markAsProcessed(Envelope envelope) {
        return handleEvent(envelope, DOC_PROCESSED);
    }

    private Boolean markAsNotified(Envelope envelope) {
        return handleEvent(envelope, DOC_PROCESSED_NOTIFICATION_SENT);
    }

    private Boolean handleEvent(Envelope envelope, Event event) {
        return errorWrapper.wrapFailure(() -> {
            envelopeProcessor.handleEvent(envelope, event);
            return Boolean.TRUE;
        });
    }

}
