package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.config.ContainerMappings;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputEnvelope;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.validation.EnvelopeValidator;
import uk.gov.hmcts.reform.bulkscanprocessor.validation.OcrValidator;
import uk.gov.hmcts.reform.bulkscanprocessor.validation.model.OcrValidationWarnings;

import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.bulkscanprocessor.model.mapper.EnvelopeMapper.toDbEnvelope;

/**
 * This class is in charge of handling input envelopes.
 * It will do below things:
 * <ol>
 * <li>Validate input envelope</li>
 * <li>Verify it did not fail to upload before</li>
 * <li>Validate its OCR data</li>
 * <li>Create DB envelope entity and save it to DB</li>
 * </ol>
 */
@Component
@EnableConfigurationProperties(ContainerMappings.class)
@ConditionalOnProperty(value = "scheduling.task.scan.enabled", matchIfMissing = true)
public class EnvelopeHandler {

    private static final Logger log = LoggerFactory.getLogger(EnvelopeHandler.class);

    private final EnvelopeValidator envelopeValidator;

    private final ContainerMappings containerMappings;

    private final EnvelopeProcessor envelopeProcessor;

    private final OcrValidator ocrValidator;

    private final boolean paymentsEnabled;

    public EnvelopeHandler(
        EnvelopeValidator envelopeValidator,
        ContainerMappings containerMappings,
        EnvelopeProcessor envelopeProcessor,
        OcrValidator ocrValidator,
        @Value("${process-payments.enabled}") boolean paymentsEnabled
    ) {
        this.envelopeValidator = envelopeValidator;
        this.containerMappings = containerMappings;
        this.envelopeProcessor = envelopeProcessor;
        this.ocrValidator = ocrValidator;
        this.paymentsEnabled = paymentsEnabled;
    }

    public void handleEnvelope(
        String containerName,
        String zipFilename,
        List<String> pdfs,
        InputEnvelope inputEnvelope
    ) {
        envelopeValidator.assertZipFilenameMatchesWithMetadata(inputEnvelope, zipFilename);
        envelopeValidator.assertContainerMatchesJurisdictionAndPoBox(
            containerMappings.getMappings(), inputEnvelope, containerName
        );
        envelopeValidator.assertServiceEnabled(inputEnvelope, containerMappings.getMappings());
        envelopeValidator.assertEnvelopeContainsOcrDataIfRequired(inputEnvelope);
        envelopeValidator.assertEnvelopeHasPdfs(inputEnvelope, pdfs);
        envelopeValidator.assertDocumentControlNumbersAreUnique(inputEnvelope);
        envelopeValidator.assertPaymentsEnabledForContainerIfPaymentsArePresent(
            inputEnvelope, paymentsEnabled, containerMappings.getMappings()
        );
        envelopeValidator.assertEnvelopeContainsDocsOfAllowedTypesForService(inputEnvelope);
        envelopeValidator.assertEnvelopeContainsDocsOfAllowedTypesOnly(inputEnvelope);

        envelopeProcessor.assertDidNotFailToUploadBefore(inputEnvelope.zipFileName, containerName);

        Optional<OcrValidationWarnings> ocrValidationWarnings = ocrValidator.assertOcrDataIsValid(
            inputEnvelope
        );

        Envelope dbEnvelope = toDbEnvelope(inputEnvelope, containerName, ocrValidationWarnings);

        log.info(
            "Envelope Name: {} case number: {}, status: {}",
            dbEnvelope.getZipFileName(),
            dbEnvelope.getCaseNumber(),
            dbEnvelope.getStatus()
        );
        envelopeProcessor.saveEnvelope(dbEnvelope);
    }
}
