package uk.gov.hmcts.reform.bulkscanprocessor.services;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobStorageException;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.FileSizeExceedMaxUploadLimit;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;
import uk.gov.hmcts.reform.bulkscanprocessor.services.storage.LeaseAcquirer;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.BlobManager;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.DocumentProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.ZipFileProcessor;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipInputStream;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.COMPLETED;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.DOC_UPLOADED;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.DOC_UPLOAD_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.FILE_SIZE_EXCEED_UPLOAD_LIMIT_FAILURE;

/**
 * Service responsible to upload envelopes ended in state after main processor task.
 * <p></p>
 * {@link uk.gov.hmcts.reform.bulkscanprocessor.tasks.BlobProcessorTask}
 * <p></p>
 * All the validations have been accomplished and {@link Envelope} saved in DB.
 * Therefore no need to go through the same set of validation steps/rules and just re-extract zip file and upload.
 * In case some {@link BlobStorageException} or {@link IOException} experienced during zip extraction
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
    private final LeaseAcquirer leaseAcquirer;

    /**
     * Constructor for the UploadEnvelopeDocumentsService.
     * @param blobManager The blob manager
     * @param zipFileProcessor The zip file processor
     * @param documentProcessor The document processor
     * @param envelopeProcessor The envelope processor
     * @param leaseAcquirer The lease acquirer
     */
    public UploadEnvelopeDocumentsService(
        BlobManager blobManager,
        ZipFileProcessor zipFileProcessor,
        DocumentProcessor documentProcessor,
        EnvelopeProcessor envelopeProcessor,
        LeaseAcquirer leaseAcquirer
    ) {
        this.blobManager = blobManager;
        this.zipFileProcessor = zipFileProcessor;
        this.documentProcessor = documentProcessor;
        this.envelopeProcessor = envelopeProcessor;
        this.leaseAcquirer = leaseAcquirer;
    }

    /**
     * Processes envelopes by container.
     * @param containerName The container name
     * @param envelopes The envelopes
     */
    public void processByContainer(String containerName, List<Envelope> envelopes) {
        log.info(
            "Uploading envelope documents from container {}. Envelopes count: {}",
            containerName,
            envelopes.size()
        );

        try {
            BlobContainerClient blobContainer = blobManager.listContainerClient(containerName);

            envelopes.forEach(envelope -> processEnvelope(blobContainer, envelope));
        } catch (Exception exception) {
            log.error(
                "An error occurred when trying to upload documents. Container: {}",
                containerName,
                exception
            );
        }
    }

    /**
     * Processes envelope.
     * @param blobContainer The blob container
     * @param envelope The envelope
     * @throws Exception If an error occurs
     */
    private void processEnvelope(BlobContainerClient blobContainer, Envelope envelope) {
        String containerName = blobContainer.getBlobContainerName();
        String zipFileName = envelope.getZipFileName();
        UUID envelopeId = envelope.getId();

        try {
            BlobClient blobClient = blobContainer.getBlobClient(zipFileName);

            leaseAcquirer.ifAcquiredOrElse(
                blobClient,
                leaseId -> uploadDocs(blobClient, envelope),
                blobErrorCode -> {},
                true
            );

        } catch (Exception exception) {
            log.error(
                "An error occurred when trying to upload documents. Container: {}, File: {}, Envelope ID: {}",
                containerName,
                zipFileName,
                envelopeId,
                exception
            );
        }
    }

    /**
     * Uploads documents.
     * @param blobClient The blob client
     * @param envelope The envelope
     */
    private void uploadDocs(BlobClient blobClient, Envelope envelope) {
        String zipFileName = envelope.getZipFileName();
        UUID envelopeId = envelope.getId();
        String containerName = blobClient.getContainerName();

        processBlobContent(blobClient, containerName, envelope);

        envelopeProcessor.handleEvent(envelope, DOC_UPLOADED);

        log.info(
            "Finished processing docs for upload. File: {}, container: {}, EnvelopeId: {}",
            zipFileName,
            containerName,
            envelopeId
        );
    }

    /**
     * Processes blob content.
     * @param blobClient The blob client
     * @param containerName The container name
     * @param envelope The envelope
     * @throws FileSizeExceedMaxUploadLimit If the file size exceeds the max upload limit
     */
    private void processBlobContent(
        BlobClient blobClient,
        String containerName,
        Envelope envelope
    ) {
        String zipFileName = envelope.getZipFileName();
        UUID envelopeId = envelope.getId();
        try (ZipInputStream zis = new ZipInputStream(blobClient.openInputStream())) {
            zipFileProcessor.extractPdfFiles(zis, zipFileName, pdfList -> uploadParsedZipFileName(envelope, pdfList));
        } catch (FileSizeExceedMaxUploadLimit exception) {
            String message = String.format(
                "PDF size exceeds max upload limit. Container: %s, File: %s, Envelope ID:  %s",
                containerName,
                zipFileName,
                envelopeId
            );
            log.error(message);
            rejectBlob(containerName, zipFileName, exception.getMessage(), envelope);
            throw new FailedUploadException(message, exception);
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

    /**
     * Uploads parsed zip file name.
     * @param envelope The envelope
     * @param pdfs The PDFs
     * @throws FailedUploadException If the upload fails
     */
    private void uploadParsedZipFileName(Envelope envelope, List<File> pdfs) {
        try {

            documentProcessor.uploadPdfFiles(
                pdfs,
                envelope.getScannableItems(),
                envelope.getJurisdiction(),
                envelope.getContainer()
            );

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

    /**
     * Creates a document upload failure event.
     * @param containerName The container name
     * @param zipFileName The zip file name
     * @param reason The reason
     * @param envelopeId The envelope ID
     */
    private void createDocUploadFailureEvent(String containerName, String zipFileName, String reason, UUID envelopeId) {
        envelopeProcessor.createEvent(
            DOC_UPLOAD_FAILURE,
            containerName,
            zipFileName,
            reason,
            envelopeId
        );
    }

    /**
     * Rejects the blob.
     * @param containerName The container name
     * @param zipFileName The zip file name
     * @param reason The reason
     * @param envelope The envelope
     */
    private void rejectBlob(String containerName, String zipFileName, String reason, Envelope envelope) {
        blobManager.tryMoveFileToRejectedContainer(zipFileName, containerName);
        envelopeProcessor.createEvent(
            FILE_SIZE_EXCEED_UPLOAD_LIMIT_FAILURE,
            containerName,
            zipFileName,
            reason,
            envelope.getId()
        );
        envelope.setStatus(COMPLETED);
        envelopeProcessor.saveEnvelope(envelope);
    }

    /**
     * Exception to be thrown when the upload fails.
     */
    private static class FailedUploadException extends RuntimeException {
        FailedUploadException(String message, Exception cause) {
            super(message, cause);
        }
    }
}
