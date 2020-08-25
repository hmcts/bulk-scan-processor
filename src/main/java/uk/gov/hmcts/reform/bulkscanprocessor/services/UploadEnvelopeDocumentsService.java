package uk.gov.hmcts.reform.bulkscanprocessor.services;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobStorageException;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.output.Pdf;
import uk.gov.hmcts.reform.bulkscanprocessor.services.storage.LeaseAcquirer;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.BlobManager;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.DocumentProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.ZipFileProcessingResult;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.ZipFileProcessor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
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

    public void processByContainer(String containerName, List<Envelope> envelopes) {
        log.info(
            "Uploading envelope documents from container {}. Envelopes count: {}",
            containerName,
            envelopes.size()
        );

        try {
            BlobContainerClient blobContainer = blobManager.getContainerClient(containerName);

            envelopes.forEach(envelope -> processEnvelope(blobContainer, envelope));
        } catch (Exception exception) {
            log.error(
                "An error occurred when trying to upload documents. Container: {}",
                containerName,
                exception
            );
        }
    }

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

            log.info(
                "Finished processing docs for upload. File: {}, container: {}, EnvelopeId: {}",
                zipFileName,
                containerName,
                envelopeId
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

    private void uploadDocs(BlobClient blobClient, Envelope envelope) {
        String zipFileName = envelope.getZipFileName();
        UUID envelopeId = envelope.getId();
        String containerName = blobClient.getContainerName();

        byte[] rawBlob =  downloadBlob(blobClient, envelopeId);

        ZipFileProcessingResult result = processBlobContent(rawBlob, containerName, zipFileName, envelopeId);

        uploadParsedZipFileName(envelope, result.getPdfs());

        envelopeProcessor.handleEvent(envelope, DOC_UPLOADED);
    }

    private byte[] downloadBlob(BlobClient blobClient, UUID envelopeId) {
        try (var outputStream = new ByteArrayOutputStream()) {
            blobClient.download(outputStream);

            return outputStream.toByteArray();
        } catch (Exception exc) {
            String message = String.format(
                "Unable to download blob. Container: %s, Blob: %s, Envelope ID: %s",
                blobClient.getContainerName(),
                blobClient.getBlobName(),
                envelopeId
            );

            throw new FailedUploadException(message, exc);
        }
    }

    private ZipFileProcessingResult processBlobContent(
        byte[] rawBlob,
        String containerName,
        String zipFileName,
        UUID envelopeId
    ) {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(rawBlob))) {
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
