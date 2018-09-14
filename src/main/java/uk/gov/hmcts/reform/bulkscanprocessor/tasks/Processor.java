package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.BlobDeleteFailureException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.EnvelopeMsg;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.output.Pdf;
import uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.ServiceBusHelper;
import uk.gov.hmcts.reform.bulkscanprocessor.services.wrapper.ErrorHandlingWrapper;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.DocumentProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;

import java.util.List;
import java.util.function.UnaryOperator;

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

    protected abstract ServiceBusHelper serviceBusHelper();

    protected void processParsedEnvelopeDocuments(
        Envelope envelope,
        List<Pdf> pdfs,
        CloudBlockBlob cloudBlockBlob,
        ServiceBusHelper serviceBusHelper
    ) {
        WrapperContext context = new WrapperContext(
            documentProcessor, envelopeProcessor, errorWrapper, serviceBusHelper, cloudBlockBlob, envelope, pdfs
        );
        Processor.uploadParsedEnvelopeDocuments(context)
            .andThen(Processor::markAsUploaded)
            .andThen(Processor::markAsProcessed)
            .andThen(Processor::sendProcessedMessage)
            .andThen(Processor::markAsNotified)
            .andThen(Processor::deleteBlob);
    }

    private static WrapperContext uploadParsedEnvelopeDocuments(WrapperContext context) {
        if (context.success) {
            context.success = context.errorWrapper.wrapDocUploadFailure(context.envelope, () -> {
                context.documentProcessor.uploadPdfFiles(context.pdfs, context.envelope.getScannableItems());
                return Boolean.TRUE;
            });
        }
        return context;
    }

    private static WrapperContext  deleteBlob(WrapperContext context) {
        if (context.success) {
            context.success = context.errorWrapper.wrapDeleteBlobFailure(context.envelope, () -> {
                // Lease needs to be broken before deleting the blob. 0 implies lease is broken immediately
                context.cloudBlockBlob.breakLease(0);
                boolean deleted = context.cloudBlockBlob.deleteIfExists();
                if (!deleted) {
                    throw new BlobDeleteFailureException(context.envelope);
                }
                context.envelope.setZipDeleted(true);
                context.envelopeProcessor.saveEnvelope(context.envelope);
                return Boolean.TRUE;
            });
        }
        return context;
    }

    private static WrapperContext sendProcessedMessage(WrapperContext context) {
        if (context.success) {
            context.success = context.errorWrapper.wrapNotificationFailure(context.envelope, () -> {
                context.serviceBusHelper.sendMessage(
                    new EnvelopeMsg(context.envelope)
                );
                return Boolean.TRUE;
            });
        }
        return context;
    }

    private static WrapperContext markAsUploaded(WrapperContext context) {
        if (context.success) {
            context.success = context.errorWrapper.wrapFailure(() -> {
                context.envelopeProcessor.handleEvent(context.envelope, DOC_UPLOADED);
                return Boolean.TRUE;
            });
        }
        return context;
    }

    private static WrapperContext markAsProcessed(WrapperContext context) {
        if (context.success) {
            context.success = context.errorWrapper.wrapFailure(() -> {
                context.envelopeProcessor.handleEvent(context.envelope, DOC_PROCESSED);
                return Boolean.TRUE;
            });
        }
        return context;
    }

    private static WrapperContext markAsNotified(WrapperContext context) {
        if (context.success) {
            context.success = context.errorWrapper.wrapFailure(() -> {
                context.envelopeProcessor.handleEvent(context.envelope, DOC_UPLOADED);
                return Boolean.TRUE;
            });
        }
        return context;
    }

    public static class WrapperContext implements UnaryOperator<WrapperContext> {
        public DocumentProcessor documentProcessor;
        public EnvelopeProcessor envelopeProcessor;
        public ErrorHandlingWrapper errorWrapper;
        public ServiceBusHelper serviceBusHelper;
        public CloudBlockBlob cloudBlockBlob;
        public Envelope envelope;
        public List<Pdf> pdfs;
        public boolean success = true;

        public WrapperContext(
            DocumentProcessor documentProcessor,
            EnvelopeProcessor envelopeProcessor,
            ErrorHandlingWrapper errorWrapper,
            ServiceBusHelper serviceBusHelper,
            CloudBlockBlob cloudBlockBlob,
            Envelope envelope,
            List<Pdf> pdfs
        ) {
            this.documentProcessor = documentProcessor;
            this.envelopeProcessor = envelopeProcessor;
            this.errorWrapper = errorWrapper;
            this.cloudBlockBlob = cloudBlockBlob;
            this.envelope = envelope;
            this.pdfs = pdfs;
            this.serviceBusHelper = serviceBusHelper;
        }

        @Override
        public WrapperContext apply(WrapperContext context) {
            return context;
        }
    }
}
