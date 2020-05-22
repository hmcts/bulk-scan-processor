package uk.gov.hmcts.reform.bulkscanprocessor.services;

import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.BlobInputStream;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.output.Pdf;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.BlobManager;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.DocumentProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.ZipFileProcessingResult;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.ZipFileProcessor;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.ZipInputStream;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.DOC_UPLOADED;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.DOC_UPLOAD_FAILURE;

/**
 * Service responsible to upload envelopes ended in state after main processor task.
 * <p></p>
 * {@link uk.gov.hmcts.reform.bulkscanprocessor.tasks.BlobProcessorTask}
 * <p></p>
 * All the validations have been accomplished and {@link Envelope} saved in DB.
 * Therefore no need to go through the same set of validation steps/rules and just re-extract zip file and upload.
 * In case some {@link StorageException} or {@link IOException} experienced during zip extraction
 * the envelope state will not be changed and left for a retry on the next run.
 * {@link Event#DOC_UPLOAD_FAILURE} can be treated as stuck envelope and be re-uploaded by same service here.
 * There is an upload retry counter/limit which may be used as well
 * Review ^ last sentence and, perhaps, incorporate such possibility here.
 */
@Service
public class UploadEnvelopeDocumentsService {

    private static final Logger log = getLogger(UploadEnvelopeDocumentsService.class);

    private final BlobManager blobManager;
    private final ZipFileProcessor zipFileProcessor;
    private final DocumentProcessor documentProcessor;
    private final EnvelopeProcessor envelopeProcessor;

    public UploadEnvelopeDocumentsService(
        BlobManager blobManager,
        ZipFileProcessor zipFileProcessor,
        DocumentProcessor documentProcessor,
        EnvelopeProcessor envelopeProcessor
    ) {
        this.blobManager = blobManager;
        this.zipFileProcessor = zipFileProcessor;
        this.documentProcessor = documentProcessor;
        this.envelopeProcessor = envelopeProcessor;
    }

    public void processByContainer(String containerName, List<Envelope> envelopes) {
        log.info(
            "Uploading envelope documents from container {}. Envelopes count: {}",
            containerName,
            envelopes.size()
        );

        try {
            CloudBlobContainer blobContainer = blobManager.getContainer(containerName);

            envelopes.forEach(envelope -> processEnvelope(blobContainer, envelope));
        } catch (Exception exception) {
            log.error(
                "An error occurred when trying to upload documents. Container: {}",
                containerName,
                exception
            );
        }
    }

    private void processEnvelope(CloudBlobContainer blobContainer, Envelope envelope) {
        String containerName = blobContainer.getName();
        String zipFileName = envelope.getZipFileName();
        UUID envelopeId = envelope.getId();
        CloudBlockBlob blob = null;
        Optional<String> leaseId = Optional.empty();

        try {
            blob = getCloudBlockBlob(blobContainer, zipFileName, envelopeId);
            leaseId = blobManager.acquireLease(blob, containerName, envelope.getZipFileName());

            if (leaseId.isPresent()) {
                BlobInputStream bis = getBlobInputStream(blob, containerName, zipFileName, envelopeId);
                ZipFileProcessingResult result = processInputStream(bis, containerName, zipFileName, envelopeId);

                uploadParsedZipFileName(envelope, result.getPdfs());

                envelopeProcessor.handleEvent(envelope, DOC_UPLOADED);
            }
        } catch (Exception exception) {
            log.error(
                "An error occurred when trying to upload documents. Container: {}, File: {}, Envelope ID: {}",
                containerName,
                zipFileName,
                envelopeId,
                exception
            );
        } finally {
            if (leaseId.isPresent()) {
                blobManager.tryReleaseLease(blob, containerName, zipFileName, leaseId.get());
            }
        }
    }

    private CloudBlockBlob getCloudBlockBlob(
        CloudBlobContainer blobContainer,
        String zipFileName,
        UUID envelopeId
    ) {
        try {
            return blobContainer.getBlockBlobReference(zipFileName);
        } catch (URISyntaxException | StorageException exception) {
            String message = String.format(
                "Unable to get blob client. Container: %s, Blob: %s, Envelope ID: %s",
                blobContainer.getName(),
                zipFileName,
                envelopeId
            );

            throw new FailedUploadException(message, exception);
        }
    }

    private BlobInputStream getBlobInputStream(
        CloudBlockBlob blobClient,
        String containerName,
        String zipFileName,
        UUID envelopeId
    ) {
        try {
            return blobClient.openInputStream();
        } catch (StorageException exception) {
            String message = String.format(
                "Unable to get blob input stream. Container: %s, Blob: %s, Envelope ID: %s",
                containerName,
                zipFileName,
                envelopeId
            );

            throw new FailedUploadException(message, exception);
        }
    }

    private ZipFileProcessingResult processInputStream(
        BlobInputStream blobInputStream,
        String containerName,
        String zipFileName,
        UUID envelopeId
    ) {
        try (ZipInputStream zis = new ZipInputStream(blobInputStream)) {
            return zipFileProcessor.process(zis, zipFileName);
        } catch (Exception exception) {
            String message = String.format(
                "Failed to process zip. File: %s, Container: %s, Envelope ID: %s",
                zipFileName,
                containerName,
                envelopeId
            );

            createDocUploadFailureEvent(containerName, zipFileName, exception.getMessage(), envelopeId);

            throw new FailedUploadException(message, exception);
        }
    }

    private void uploadParsedZipFileName(Envelope envelope, List<Pdf> pdfs) {
        try {
            documentProcessor.uploadPdfFiles(pdfs, envelope.getScannableItems());

            log.info(
                "Uploaded PDF files to Document Management. File {}, Container: {}, Envelope ID: {}",
                envelope.getZipFileName(),
                envelope.getContainer(),
                envelope.getId()
            );
        } catch (Exception exception) {
            String message = String.format(
                "Failed to upload PDF files to Document Management. File: %s, Container: %s, Envelope ID: %s",
                envelope.getZipFileName(),
                envelope.getContainer(),
                envelope.getId()
            );

            envelopeProcessor.markAsUploadFailure(envelope);
            createDocUploadFailureEvent(
                envelope.getContainer(),
                envelope.getZipFileName(),
                exception.getMessage(),
                envelope.getId()
            );

            throw new FailedUploadException(message, exception);
        }
    }

    private void createDocUploadFailureEvent(String containerName, String zipFileName, String reason, UUID envelopeId) {
        envelopeProcessor.createEvent(
            DOC_UPLOAD_FAILURE,
            containerName,
            zipFileName,
            reason,
            envelopeId
        );
    }

    private static class FailedUploadException extends RuntimeException {
        FailedUploadException(String message, Exception cause) {
            super(message, cause);
        }
    }
}
