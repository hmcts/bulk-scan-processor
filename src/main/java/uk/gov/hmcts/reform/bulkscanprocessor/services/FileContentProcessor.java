package uk.gov.hmcts.reform.bulkscanprocessor.services;

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
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.ZipFileContentDetail;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.ZipFileProcessor;

import java.util.Optional;
import java.util.zip.ZipInputStream;

import static java.util.stream.Collectors.joining;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.DISABLED_SERVICE_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.DOC_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.DOC_UPLOAD_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.FILE_VALIDATION_FAILURE;

/**
 * Processes the content of a zip file.
 */
@Component
@ConditionalOnProperty(value = "scheduling.task.scan.enabled", matchIfMissing = true)
@ConditionalOnExpression("!${jms.enabled}")
public class FileContentProcessor {
    private static final Logger log = LoggerFactory.getLogger(FileContentProcessor.class);

    private final ZipFileProcessor zipFileProcessor;

    private final EnvelopeProcessor envelopeProcessor;

    private final EnvelopeHandler envelopeHandler;

    private final FileRejector fileRejector;

    private static final String CASE_REFERENCE_NOT_PRESENT = "(NOT PRESENT)";

    /**
     * Constructor for the FileContentProcessor.
     * @param zipFileProcessor The zip file processor
     * @param envelopeProcessor The envelope processor
     * @param envelopeHandler The envelope handler
     * @param fileRejector The file rejector
     */
    public FileContentProcessor(
        ZipFileProcessor zipFileProcessor,
        EnvelopeProcessor envelopeProcessor,
        EnvelopeHandler envelopeHandler,
        FileRejector fileRejector
    ) {
        this.zipFileProcessor = zipFileProcessor;
        this.envelopeProcessor = envelopeProcessor;
        this.envelopeHandler = envelopeHandler;
        this.fileRejector = fileRejector;
    }

    /**
     * Processes the content of a zip file.
     * @param zis The zip input stream
     * @param zipFilename The zip file name
     * @param containerName The container name
     */
    public void processZipFileContent(
        ZipInputStream zis,
        String zipFilename,
        String containerName
    ) {
        Optional<String> caseReference = Optional.empty();
        try {
            ZipFileContentDetail zipDetail = zipFileProcessor.getZipContentDetail(zis, zipFilename);

            InputEnvelope inputEnvelope = envelopeProcessor.parseEnvelope(zipDetail.getMetadata(), zipFilename);
            caseReference = Optional.ofNullable(inputEnvelope.caseNumber);

            log.info(
                "Parsed envelope. File name: {}. Container: {}. Payment DCNs: {}. Document DCNs: {}, caseNumber {}",
                zipFilename,
                containerName,
                inputEnvelope.payments.stream().map(payment -> payment.documentControlNumber).collect(joining(",")),
                inputEnvelope.scannableItems.stream().map(doc -> doc.documentControlNumber).collect(joining(",")),
                caseReference.orElse(CASE_REFERENCE_NOT_PRESENT)
            );

            envelopeHandler.handleEnvelope(
                containerName,
                zipFilename,
                zipDetail.pdfFileNames,
                inputEnvelope
            );
        } catch (PaymentsDisabledException ex) {
            log.error(
                "Rejected file {} from container {}, Case reference: {} - Payments processing is disabled",
                zipFilename, containerName, caseReference.orElse(CASE_REFERENCE_NOT_PRESENT)
            );
            Long eventId = createEvent(FILE_VALIDATION_FAILURE, containerName, zipFilename, ex.getMessage());
            fileRejector.handleInvalidBlob(eventId, containerName, zipFilename, ex);
        } catch (ServiceDisabledException ex) {
            log.error(
                "Rejected file {} from container {}, Case reference: {} - Service is disabled",
                zipFilename, containerName, caseReference.orElse(CASE_REFERENCE_NOT_PRESENT)
            );
            Long eventId = createEvent(DISABLED_SERVICE_FAILURE, containerName, zipFilename, ex.getMessage());
            fileRejector.handleInvalidBlob(eventId, containerName, zipFilename, ex);
        } catch (EnvelopeRejectionException ex) {
            log.warn("Rejected file {} from container {}, Case reference: {} - invalid",
                     zipFilename, containerName, caseReference.orElse(CASE_REFERENCE_NOT_PRESENT), ex);
            Long eventId = createEvent(FILE_VALIDATION_FAILURE, containerName, zipFilename, ex.getMessage());
            fileRejector.handleInvalidBlob(eventId, containerName, zipFilename, ex);
        } catch (PreviouslyFailedToUploadException ex) {
            log.warn("Rejected file {} from container {}, Case reference: {} - failed previously",
                     zipFilename, containerName, caseReference.orElse(CASE_REFERENCE_NOT_PRESENT), ex);
            createEvent(DOC_UPLOAD_FAILURE, containerName, zipFilename, ex.getMessage());
        } catch (Exception ex) {
            log.error("Failed to process file {} from container {}", zipFilename, containerName, ex);
            createEvent(DOC_FAILURE, containerName, zipFilename, ex.getMessage());
        }
    }

    /**
     * Creates an event.
     * @param event The event
     * @param containerName The container name
     * @param zipFilename The zip file name
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
