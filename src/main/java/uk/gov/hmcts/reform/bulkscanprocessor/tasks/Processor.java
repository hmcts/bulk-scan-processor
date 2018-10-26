package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.microsoft.azure.storage.blob.BlockBlobURL;
import com.microsoft.rest.v2.Context;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Event;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.EnvelopeMsg;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.output.Pdf;
import uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.ServiceBusHelper;
import uk.gov.hmcts.reform.bulkscanprocessor.services.wrapper.ErrorHandlingWrapper;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.DocumentProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.util.AzureStorageHelper;

import java.util.List;

import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Event.DOC_PROCESSED;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Event.DOC_PROCESSED_NOTIFICATION_SENT;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Event.DOC_UPLOADED;

public abstract class Processor {

    protected final AzureStorageHelper azureStorageHelper;
    private final DocumentProcessor documentProcessor;
    protected final EnvelopeProcessor envelopeProcessor;
    protected final ErrorHandlingWrapper errorWrapper;
    protected String signatureAlg;
    protected String publicKeyDerFilename;


    protected Processor(
        AzureStorageHelper azureStorageHelper,
        DocumentProcessor documentProcessor,
        EnvelopeProcessor envelopeProcessor,
        ErrorHandlingWrapper errorWrapper,
        String signatureAlg,
        String publicKeyDerFilename
    ) {
        this.azureStorageHelper = azureStorageHelper;
        this.documentProcessor = documentProcessor;
        this.envelopeProcessor = envelopeProcessor;
        this.errorWrapper = errorWrapper;
        this.signatureAlg = signatureAlg;
        this.publicKeyDerFilename = publicKeyDerFilename;
    }

    protected abstract ServiceBusHelper serviceBusHelper();

    protected void processParsedEnvelopeDocuments(
        Envelope envelope,
        List<Pdf> pdfs,
        BlockBlobURL blockBlobURL,
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
        deleteBlob(envelope, blockBlobURL);
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
        BlockBlobURL blockBlobURL
    ) {
        return errorWrapper.wrapDeleteBlobFailure(envelope, () -> {
            // Lease needs to be broken before deleting the blob. 0 implies lease is broken immediately

            blockBlobURL.breakLease(0, null, Context.NONE)
                .flatMap(res -> blockBlobURL.delete(null, null, Context.NONE))
                .blockingGet();

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
