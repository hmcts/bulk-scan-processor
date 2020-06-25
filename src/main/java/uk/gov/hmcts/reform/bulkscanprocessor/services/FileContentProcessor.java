package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.config.ContainerMappings;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EnvelopeRejectionException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.PaymentsDisabledException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.PreviouslyFailedToUploadException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ServiceDisabledException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputEnvelope;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.ZipFileProcessingResult;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.ZipFileProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.validation.EnvelopeValidator;
import uk.gov.hmcts.reform.bulkscanprocessor.validation.OcrValidator;
import uk.gov.hmcts.reform.bulkscanprocessor.validation.model.OcrValidationWarnings;

import java.util.Optional;
import java.util.zip.ZipInputStream;

import static java.util.stream.Collectors.joining;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.DISABLED_SERVICE_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.FILE_VALIDATION_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.mapper.EnvelopeMapper.toDbEnvelope;

/**
 * This class is in charge of processing content of zip file as an InputStream.
 * It will do below things:
 * <ol>
 * <li>Run necessary validations</li>
 * <li>Transform metadata json to DB entities</li>
 * <li>Save PDF files in document storage</li>
 * <li>Update status and doc urls in DB</li>
 * </ol>
 */
@Component
@ConditionalOnProperty(value = "scheduling.task.scan.enabled", matchIfMissing = true)
public class FileContentProcessor {
    private static final Logger log = LoggerFactory.getLogger(FileContentProcessor.class);

    private final EnvelopeProcessor envelopeProcessor;

    private final ZipFileProcessor zipFileProcessor;

    private final ContainerMappings containerMappings;

    private final OcrValidator ocrValidator;

    private final EnvelopeValidator envelopeValidator;

    private final FileErrorHandler fileErrorHandler;

    private final boolean paymentsEnabled;

    public FileContentProcessor(
        EnvelopeProcessor envelopeProcessor,
        ZipFileProcessor zipFileProcessor,
        ContainerMappings containerMappings,
        OcrValidator ocrValidator,
        EnvelopeValidator envelopeValidator,
        FileErrorHandler fileErrorHandler,
        @Value("${process-payments.enabled}") boolean paymentsEnabled
    ) {
        this.envelopeProcessor = envelopeProcessor;
        this.zipFileProcessor = zipFileProcessor;
        this.containerMappings = containerMappings;
        this.ocrValidator = ocrValidator;
        this.envelopeValidator = envelopeValidator;
        this.fileErrorHandler = fileErrorHandler;
        this.paymentsEnabled = paymentsEnabled;
    }

    public void processZipFileContent(
        ZipInputStream zis,
        String zipFilename,
        String containerName,
        String leaseId
    ) {
        try {
            ZipFileProcessingResult result = zipFileProcessor.process(zis, zipFilename);

            InputEnvelope envelope = envelopeProcessor.parseEnvelope(result.getMetadata(), zipFilename);

            log.info(
                "Parsed envelope. File name: {}. Container: {}. Payment DCNs: {}. Document DCNs: {}",
                zipFilename,
                containerName,
                envelope.payments.stream().map(payment -> payment.documentControlNumber).collect(joining(",")),
                envelope.scannableItems.stream().map(doc -> doc.documentControlNumber).collect(joining(","))
            );

            envelopeValidator.assertZipFilenameMatchesWithMetadata(envelope, zipFilename);
            envelopeValidator.assertContainerMatchesJurisdictionAndPoBox(
                containerMappings.getMappings(), envelope, containerName
            );
            envelopeValidator.assertServiceEnabled(envelope, containerMappings.getMappings());
            envelopeValidator.assertEnvelopeContainsOcrDataIfRequired(envelope);
            envelopeValidator.assertEnvelopeHasPdfs(envelope, result.getPdfs());
            envelopeValidator.assertDocumentControlNumbersAreUnique(envelope);
            envelopeValidator.assertPaymentsEnabledForContainerIfPaymentsArePresent(
                envelope, paymentsEnabled, containerMappings.getMappings()
            );
            envelopeValidator.assertEnvelopeContainsDocsOfAllowedTypesOnly(envelope);

            envelopeProcessor.assertDidNotFailToUploadBefore(envelope.zipFileName, containerName);

            Optional<OcrValidationWarnings> ocrValidationWarnings = ocrValidator.assertOcrDataIsValid(envelope);

            Envelope dbEnvelope = toDbEnvelope(envelope, containerName, ocrValidationWarnings);

            envelopeProcessor.saveEnvelope(dbEnvelope);
        } catch (PaymentsDisabledException ex) {
            log.error(
                "Rejected file {} from container {} - Payments processing is disabled", zipFilename, containerName
            );
            fileErrorHandler.handleInvalidFileError(FILE_VALIDATION_FAILURE, containerName, zipFilename, leaseId, ex);
        } catch (ServiceDisabledException ex) {
            log.error(
                "Rejected file {} from container {} - Service is disabled", zipFilename, containerName
            );
            fileErrorHandler.handleInvalidFileError(DISABLED_SERVICE_FAILURE, containerName, zipFilename, leaseId, ex);
        } catch (EnvelopeRejectionException ex) {
            log.warn("Rejected file {} from container {} - invalid", zipFilename, containerName, ex);
            fileErrorHandler.handleInvalidFileError(FILE_VALIDATION_FAILURE, containerName, zipFilename, leaseId, ex);
        } catch (PreviouslyFailedToUploadException ex) {
            log.warn("Rejected file {} from container {} - failed previously", zipFilename, containerName, ex);
            createEvent(Event.DOC_UPLOAD_FAILURE, containerName, zipFilename, ex);
        } catch (Exception ex) {
            log.error("Failed to process file {} from container {}", zipFilename, containerName, ex);
            createEvent(Event.DOC_FAILURE, containerName, zipFilename, ex);
        }
    }

    private void createEvent(
        Event event,
        String containerName,
        String zipFilename,
        Exception exception
    ) {
        envelopeProcessor.createEvent(
            event,
            containerName,
            zipFilename,
            exception.getMessage(),
            null
        );
    }
}
