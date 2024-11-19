package uk.gov.hmcts.reform.bulkscanprocessor.services.jms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EnvelopeRejectionException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.PaymentsDisabledException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.PreviouslyFailedToUploadException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ServiceDisabledException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputEnvelope;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;
import uk.gov.hmcts.reform.bulkscanprocessor.services.EnvelopeHandler;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.ZipFileContentDetail;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.ZipFileProcessor;

import java.util.zip.ZipInputStream;

import static java.util.stream.Collectors.joining;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.DISABLED_SERVICE_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.DOC_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.DOC_UPLOAD_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.FILE_VALIDATION_FAILURE;

/**
 * Processes the content of a zip file received from the message queue.
 */
@Component
@ConditionalOnProperty(value = "scheduling.task.scan.enabled", matchIfMissing = true)
@ConditionalOnExpression("${jms.enabled}")
public class JmsFileContentProcessor {
    private static final Logger log = LoggerFactory.getLogger(JmsFileContentProcessor.class);

    private final ZipFileProcessor zipFileProcessor;

    private final EnvelopeProcessor envelopeProcessor;

    private final EnvelopeHandler envelopeHandler;

    private final JmsFileRejector fileRejector;

    /**
     * Constructor for JmsFileContentProcessor.
     * @param zipFileProcessor The zip file processor
     * @param envelopeProcessor The envelope processor
     * @param envelopeHandler The envelope handler
     * @param fileRejector The file rejector
     */
    public JmsFileContentProcessor(
        ZipFileProcessor zipFileProcessor,
        EnvelopeProcessor envelopeProcessor,
        EnvelopeHandler envelopeHandler,
        JmsFileRejector fileRejector
    ) {
        this.envelopeProcessor = envelopeProcessor;
        this.envelopeHandler = envelopeHandler;
        this.fileRejector = fileRejector;
        this.zipFileProcessor = zipFileProcessor;
    }

    /**
     * Processes the content of a zip file.
     * @param zis The zip input stream
     * @param zipFilename The zip filename
     * @param containerName The container name
     */
    public void processZipFileContent(
        ZipInputStream zis,
        String zipFilename,
        String containerName
    ) {

        String caseReference = null;
        try {
            ZipFileContentDetail zipDetail = zipFileProcessor.getZipContentDetail(zis, zipFilename);

            InputEnvelope inputEnvelope = envelopeProcessor.parseEnvelope(zipDetail.getMetadata(), zipFilename);
            caseReference = inputEnvelope.caseNumber;

            log.info(
                "Parsed envelope. File name: {}. Container: {}. Payment DCNs: {}. Document DCNs: {}, caseNumber {}",
                zipFilename,
                containerName,
                inputEnvelope.payments.stream().map(payment -> payment.documentControlNumber).collect(joining(",")),
                inputEnvelope.scannableItems.stream().map(doc -> doc.documentControlNumber).collect(joining(",")),
                inputEnvelope.caseNumber
            );

            envelopeHandler.handleEnvelope(
                containerName,
                zipFilename,
                zipDetail.pdfFileNames,
                inputEnvelope
            );
        } catch (PaymentsDisabledException ex) {
            log.error(
                "Rejected file {} from container {}, Case reference (if any): {} - Payments processing is disabled",
                zipFilename, containerName, caseReference
            );
            Long eventId = createEvent(FILE_VALIDATION_FAILURE, containerName, zipFilename, ex.getMessage());
            fileRejector.handleInvalidBlob(eventId, containerName, zipFilename, ex);
        } catch (ServiceDisabledException ex) {
            log.error(
                "Rejected file {} from container {}, Case reference (if any): {} - Service is disabled",
                zipFilename, containerName, caseReference
            );
            Long eventId = createEvent(DISABLED_SERVICE_FAILURE, containerName, zipFilename, ex.getMessage());
            fileRejector.handleInvalidBlob(eventId, containerName, zipFilename, ex);
        }  catch (PreviouslyFailedToUploadException ex) {
            log.warn("Rejected file {} from container {}, Case reference (if any): {} - failed previously",
                     zipFilename, containerName, caseReference, ex);
            createEvent(DOC_UPLOAD_FAILURE, containerName, zipFilename, ex.getMessage());
        } catch (EnvelopeRejectionException ex) {
            log.warn("Rejected file {} from container {}, Case reference (if any): {} - invalid",
                     zipFilename, containerName, caseReference, ex);
            Long eventId = createEvent(FILE_VALIDATION_FAILURE, containerName, zipFilename, ex.getMessage());
            fileRejector.handleInvalidBlob(eventId, containerName, zipFilename, ex);
        } catch (Exception ex) {
            log.error("Failed to process file {} from container {}", zipFilename, containerName, ex);
            createEvent(DOC_FAILURE, containerName, zipFilename, ex.getMessage());
        }
    }

    /**
     * Creates an event.
     * @param event The event
     * @param containerName The container name
     * @param zipFilename The zip filename
     * @param message The message
     * @return The event ID
     */
    private Long createEvent(
        Event event,
        String containerName,
        String zipFilename,
        String message
    ) {
        return envelopeProcessor.createEvent(
            event,
            containerName,
            zipFilename,
            message,
            null
        );
    }
}
