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
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Event.DOC_PROCESSED_NOTIFICATION_SENT;
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
            serviceBusHelper, cloudBlockBlob, envelope, pdfs
        );

        new WrapperContextProcessor()
            .andThen(this::uploadParsedEnvelopeDocuments)
            .andThen(this::markAsUploaded)
            .andThen(this::markAsProcessed)
            .andThen(this::sendProcessedMessage)
            .andThen(this::markAsNotified)
            .andThen(this::deleteBlobIfProcessed)
            .apply(context);
    }

    private WrapperContext uploadParsedEnvelopeDocuments(WrapperContext context) {
        if (context.success) {
            context.success = errorWrapper.wrapDocUploadFailure(context.envelope, () -> {
                documentProcessor.uploadPdfFiles(context.pdfs, context.envelope.getScannableItems());
                return Boolean.TRUE;
            });
        }
        return context;
    }

    private WrapperContext deleteBlobIfProcessed(WrapperContext context) {
        if (context.envelope.getStatus().isProcessed()) {
            errorWrapper.wrapDeleteBlobFailure(context.envelope, () -> {
                // Lease needs to be broken before deleting the blob. 0 implies lease is broken immediately
                context.cloudBlockBlob.breakLease(0);
                boolean deleted = context.cloudBlockBlob.deleteIfExists();
                if (!deleted) {
                    throw new BlobDeleteFailureException(context.envelope);
                }
                context.envelope.setZipDeleted(true);
                envelopeProcessor.saveEnvelope(context.envelope);
                return Boolean.TRUE;
            });
        }
        return context;
    }

    private WrapperContext sendProcessedMessage(WrapperContext context) {
        if (context.success) {
            context.success = errorWrapper.wrapNotificationFailure(context.envelope, () -> {
                context.serviceBusHelper.sendMessage(
                    new EnvelopeMsg(context.envelope)
                );
                return Boolean.TRUE;
            });
        }
        return context;
    }

    private WrapperContext markAsUploaded(WrapperContext context) {
        if (context.success) {
            context.success = errorWrapper.wrapGenericFailure(() -> {
                envelopeProcessor.handleEvent(context.envelope, DOC_UPLOADED);
                return Boolean.TRUE;
            });
        }
        return context;
    }

    private WrapperContext markAsProcessed(WrapperContext context) {
        if (context.success) {
            context.success = errorWrapper.wrapGenericFailure(() -> {
                envelopeProcessor.handleEvent(context.envelope, DOC_PROCESSED);
                return Boolean.TRUE;
            });
        }
        return context;
    }

    private WrapperContext markAsNotified(WrapperContext context) {
        if (context.success) {
            context.success = errorWrapper.wrapGenericFailure(() -> {
                envelopeProcessor.handleEvent(context.envelope, DOC_PROCESSED_NOTIFICATION_SENT);
                return Boolean.TRUE;
            });
        }
        return context;
    }

    public static class WrapperContext {
        public final ServiceBusHelper serviceBusHelper;
        public final CloudBlockBlob cloudBlockBlob;
        public final Envelope envelope;
        public final List<Pdf> pdfs;
        public boolean success = true;

        public WrapperContext(
            ServiceBusHelper serviceBusHelper,
            CloudBlockBlob cloudBlockBlob,
            Envelope envelope,
            List<Pdf> pdfs
        ) {
            this.serviceBusHelper = serviceBusHelper;
            this.cloudBlockBlob = cloudBlockBlob;
            this.envelope = envelope;
            this.pdfs = pdfs;
        }
    }

    public static class WrapperContextProcessor implements UnaryOperator<WrapperContext> {
        @Override
        public WrapperContext apply(WrapperContext context) {
            return context;
        }
    }

}
