package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.config.ContainerMappings;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.PaymentsDisabledException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ServiceDisabledException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputEnvelope;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.output.Pdf;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.validation.EnvelopeValidator;
import uk.gov.hmcts.reform.bulkscanprocessor.validation.OcrValidator;
import uk.gov.hmcts.reform.bulkscanprocessor.validation.model.OcrValidationWarnings;

import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.DISABLED_SERVICE_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.FILE_VALIDATION_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.mapper.EnvelopeMapper.toDbEnvelope;

@Component
@EnableConfigurationProperties(ContainerMappings.class)
@ConditionalOnProperty(value = "scheduling.task.scan.enabled", matchIfMissing = true)
public class EnvelopeHandler {

    private static final Logger log = LoggerFactory.getLogger(EnvelopeHandler.class);

    private final EnvelopeValidator envelopeValidator;

    private final ContainerMappings containerMappings;

    private final EnvelopeProcessor envelopeProcessor;

    private final boolean paymentsEnabled;

    private final OcrValidator ocrValidator;

    private final FileErrorHandler fileErrorHandler;

    public EnvelopeHandler(
        EnvelopeValidator envelopeValidator,
        ContainerMappings containerMappings,
        EnvelopeProcessor envelopeProcessor,
        OcrValidator ocrValidator,
        FileErrorHandler fileErrorHandler,
        @Value("${process-payments.enabled}") boolean paymentsEnabled
    ) {
        this.envelopeValidator = envelopeValidator;
        this.containerMappings = containerMappings;
        this.envelopeProcessor = envelopeProcessor;
        this.ocrValidator = ocrValidator;
        this.fileErrorHandler = fileErrorHandler;
        this.paymentsEnabled = paymentsEnabled;
    }

    public void handleEnvelope(
        String containerName,
        String zipFilename,
        List<Pdf> pdfs,
        InputEnvelope envelope,
        String leaseId
    ) {
        try {

            envelopeValidator.assertZipFilenameMatchesWithMetadata(envelope, zipFilename);
            envelopeValidator.assertContainerMatchesJurisdictionAndPoBox(
                containerMappings.getMappings(), envelope, containerName
            );
            envelopeValidator.assertServiceEnabled(envelope, containerMappings.getMappings());
            envelopeValidator.assertEnvelopeContainsOcrDataIfRequired(envelope);
            envelopeValidator.assertEnvelopeHasPdfs(envelope, pdfs);
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
        }
    }
}
