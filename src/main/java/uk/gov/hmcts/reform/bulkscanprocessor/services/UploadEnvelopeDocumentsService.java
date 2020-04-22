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
        log.info("Processing envelopes in {} container. Envelopes found: {}", containerName, envelopes.size());

        try {
            CloudBlobContainer blobContainer = blobManager.getContainer(containerName);

            envelopes.forEach(envelope -> {
                Optional<CloudBlockBlob> blobClient = getCloudBlockBlob(
                    blobContainer,
                    envelope.getZipFileName(),
                    envelope.getId()
                );

                blobClient
                    .flatMap(client -> blobManager
                        .acquireLease(client, containerName, envelope.getZipFileName())
                        .map(voidLeaseId -> client)
                    )
                    .flatMap(client ->
                        getBlobInputStream(client, containerName, envelope.getZipFileName(), envelope.getId())
                    ).flatMap(inputStream ->
                        processInputStream(inputStream, containerName, envelope.getZipFileName(), envelope.getId())
                    ).map(result ->
                        uploadParsedZipFileName(envelope, result.getPdfs())
                    ).filter(isUploaded -> isUploaded).ifPresent(voidTrue ->
                        envelopeProcessor.handleEvent(envelope, DOC_UPLOADED)
                    );

                blobClient.ifPresent(this::breakLease);
            });
        } catch (URISyntaxException | StorageException exception) {
            log.error("Unable to get client for {} container", containerName, exception);
        }
    }

    private Optional<CloudBlockBlob> getCloudBlockBlob(
        CloudBlobContainer blobContainer,
        String zipFileName,
        UUID envelopeId
    ) {
        try {
            return Optional.of(blobContainer.getBlockBlobReference(zipFileName));
        } catch (URISyntaxException | StorageException exception) {
            log.error(
                "Unable to get blob client. Container: {}, Blob: {}, Envelope ID: {}",
                blobContainer.getName(),
                zipFileName,
                envelopeId,
                exception
            );

            return Optional.empty();
        }
    }

    private Optional<BlobInputStream> getBlobInputStream(
        CloudBlockBlob blobClient,
        String containerName,
        String zipFileName,
        UUID envelopeId
    ) {
        try {
            return Optional.of(blobClient.openInputStream());
        } catch (StorageException exception) {
            log.error(
                "Unable to get blob input stream. Container: {}, Blob: {}, Envelope ID: {}",
                containerName,
                zipFileName,
                envelopeId,
                exception
            );

            return Optional.empty();
        }
    }

    private Optional<ZipFileProcessingResult> processInputStream(
        BlobInputStream blobInputStream,
        String containerName,
        String zipFileName,
        UUID envelopeId
    ) {
        try (ZipInputStream zis = new ZipInputStream(blobInputStream)) {
            return Optional.of(
                zipFileProcessor.process(zis, zipFileName)
            );
        } catch (Exception exception) {
            log.error(
                "Failed to process zip. File: {}, Container: {}, Envelope ID: {}",
                zipFileName,
                containerName,
                envelopeId,
                exception
            );

            createDocUploadFailureEvent(containerName, zipFileName, exception.getMessage(), envelopeId);
        }

        return Optional.empty();
    }

    private boolean uploadParsedZipFileName(Envelope envelope, List<Pdf> pdfs) {
        try {
            documentProcessor.uploadPdfFiles(pdfs, envelope.getScannableItems());

            log.info(
                "Uploaded pdfs. File {}, Container: {}, Envelope ID: {}",
                envelope.getZipFileName(),
                envelope.getContainer(),
                envelope.getId()
            );

            return true;
        } catch (Exception exception) {
            log.error(
                "Failed to upload PDF files to Document Management. File: {}, Container: {}, Envelope ID: {}",
                envelope.getZipFileName(),
                envelope.getContainer(),
                envelope.getId(),
                exception
            );

            envelopeProcessor.markAsUploadFailure(envelope);
            createDocUploadFailureEvent(
                envelope.getContainer(),
                envelope.getZipFileName(),
                exception.getMessage(),
                envelope.getId()
            );

            return false;
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

    private void breakLease(CloudBlockBlob blobClient) {
        try {
            blobClient.breakLease(0);
        } catch (StorageException exception) {
            // we will expire lease anyway. no need to escalate to error
            log.warn("Failed to break the lease for {}", blobClient.getName(), exception);
        }
    }
}
