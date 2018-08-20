package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import org.hibernate.validator.HibernateValidator;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.FileNameIrregularitiesException;
import uk.gov.hmcts.reform.bulkscanprocessor.services.wrapper.ErrorHandlingWrapper;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.DocumentProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.ZipFileProcessor;

import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

public abstract class Processor {

    protected final CloudBlobClient cloudBlobClient;
    private final DocumentProcessor documentProcessor;
    protected final EnvelopeProcessor envelopeProcessor;
    protected final ErrorHandlingWrapper errorWrapper;

    private final Validator validator;

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

        validator = Validation
            .byProvider(HibernateValidator.class)
            .configure()
            .buildValidatorFactory()
            .getValidator();
    }

    protected void processParsedEnvelopeDocuments(
        ZipFileProcessor zipFileProcessor,
        CloudBlockBlob cloudBlockBlob
    ) {
        Envelope envelope = zipFileProcessor.getEnvelope();

        errorWrapper.wrapDocUploadFailure(envelope, () -> {
            validateZipFileProcessor(zipFileProcessor);

            documentProcessor.processPdfFiles(zipFileProcessor.getPdfs(), envelope.getScannableItems());
            envelopeProcessor.markAsUploaded(envelope);

            cloudBlockBlob.delete();

            envelopeProcessor.markAsProcessed(envelope);

            return null;
        });
    }

    private void validateZipFileProcessor(ZipFileProcessor zipFileProcessor) {
        Set<ConstraintViolation<ZipFileProcessor>> violations = validator.validate(zipFileProcessor);

        if (!violations.isEmpty()) {
            ConstraintViolation<ZipFileProcessor> violation = violations.iterator().next();

            throw new FileNameIrregularitiesException(zipFileProcessor.getEnvelope(), violation.getMessage());
        }
    }
}
