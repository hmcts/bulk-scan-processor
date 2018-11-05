package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.BlobInputStream;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.LeaseStatus;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Event;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EventRelatedThrowable;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.DocumentProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.ZipFileProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.ZipVerifiers;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipInputStream;

/**
 * This class is a task executed by Scheduler as per configured interval.
 * It will read all the blobs from Azure Blob storage and will do below things:
 * <ol>
 * <li>Read Blob from container by acquiring lease</li>
 * <li>Extract Zip file (blob)</li>
 * <li>Transform metadata json to DB entities</li>
 * <li>Save PDF files in document storage</li>
 * <li>Update status and doc urls in DB</li>
 * </ol>
 */
@Component
@ConditionalOnProperty(value = "scheduling.task.scan.enabled", matchIfMissing = true)
public class BlobProcessorTask extends Processor {

    private static final Logger log = LoggerFactory.getLogger(BlobProcessorTask.class);

    @Value("${storage.blob_lease_timeout}")
    private Integer blobLeaseTimeout;

    @Value("${storage.blob_processing_delay_in_minutes}")
    protected int blobProcessingDelayInMinutes = 0;


    @Autowired
    public BlobProcessorTask(
        CloudBlobClient cloudBlobClient,
        DocumentProcessor documentProcessor,
        EnvelopeProcessor envelopeProcessor,
        EnvelopeRepository envelopeRepository,
        ProcessEventRepository eventRepository
    ) {
        super(cloudBlobClient, documentProcessor, envelopeProcessor, envelopeRepository, eventRepository);
    }

    // NOTE: this is needed for testing as children of this class are instantiated
    // using "new" in tests despite being spring beans (sigh!)
    public BlobProcessorTask(
        CloudBlobClient cloudBlobClient,
        DocumentProcessor documentProcessor,
        EnvelopeProcessor envelopeProcessor,
        EnvelopeRepository envelopeRepository,
        ProcessEventRepository eventRepository,
        String signatureAlg,
        String publicKeyDerFilename
    ) {
        this(cloudBlobClient, documentProcessor, envelopeProcessor, envelopeRepository, eventRepository);
        this.signatureAlg = signatureAlg;
        this.publicKeyDerFilename = publicKeyDerFilename;
    }

    @Scheduled(fixedDelayString = "${scheduling.task.scan.delay}")
    public void processBlobs() throws IOException, StorageException, URISyntaxException {
        for (CloudBlobContainer container : cloudBlobClient.listContainers()) {
            processZipFiles(container);
        }
    }

    private void processZipFiles(CloudBlobContainer container)
        throws IOException, StorageException, URISyntaxException {
        log.info("Processing blobs for container {}", container.getName());

        // Randomise iteration order to minimise lease acquire contention
        // For this purpose it's more efficient to have a collection that
        // implements RandomAccess (e.g. ArrayList)
        List<String> zipFilenames = new ArrayList<>();
        container.listBlobs().forEach(
            b -> zipFilenames.add(FilenameUtils.getName(b.getUri().toString()))
        );
        Collections.shuffle(zipFilenames);
        for (String zipFilename : zipFilenames) {
            processZipFile(container, zipFilename);
        }
    }

    private void processZipFile(CloudBlobContainer container, String zipFilename)
        throws IOException, StorageException, URISyntaxException {

        CloudBlockBlob cloudBlockBlob = container.getBlockBlobReference(zipFilename);
        cloudBlockBlob.downloadAttributes();

        if (!isReadyToBeProcessed(cloudBlockBlob)) {
            return;
        }

        log.info("Processing zip file {}", zipFilename);

        Envelope existingEnvelope =
            envelopeProcessor.getEnvelopeByFileAndContainer(container.getName(), zipFilename);
        if (existingEnvelope != null) {
            deleteIfProcessed(cloudBlockBlob, existingEnvelope);
            return;
        }

        CloudBlockBlob blobWithLeaseAcquired = acquireLease(cloudBlockBlob, container.getName(), zipFilename);

        if (Objects.nonNull(blobWithLeaseAcquired)) {
            BlobInputStream blobInputStream = blobWithLeaseAcquired.openInputStream();

            // Zip file will include metadata.json and collection of pdf documents
            try (ZipInputStream zis = new ZipInputStream(blobInputStream)) {
                ZipFileProcessor zipFileProcessor = processZipFileContent(zis, zipFilename, container.getName());

                if (zipFileProcessor != null) {
                    processParsedEnvelopeDocuments(
                        zipFileProcessor.getEnvelope(),
                        zipFileProcessor.getPdfs(),
                        blobWithLeaseAcquired
                    );
                }
            }
        }
    }

    private void deleteIfProcessed(CloudBlockBlob cloudBlockBlob, Envelope envelope) {
        try {
            if (cloudBlockBlob != null && envelope.getStatus().isProcessed()) {
                boolean deleted;
                if (cloudBlockBlob.exists()) {
                    deleted = cloudBlockBlob.deleteIfExists();
                } else {
                    deleted = true;
                }
                if (deleted) {
                    envelope.setZipDeleted(true);
                    envelopeProcessor.saveEnvelope(envelope);
                }
            }
        } catch (StorageException e) {
            log.warn("Failed to delete blob [{}]", cloudBlockBlob.getName());
        } // Do not propagate exception as this blob has already been marked with a delete failure
    }

    private CloudBlockBlob acquireLease(CloudBlockBlob cloudBlockBlob, String containerName, String zipFilename) {
        try {
            // Note: trying to lease an already leased blob throws an exception and
            // we really do not want to fill the application logs with these. Unfortunately
            // even with this check there is still a chance of an exception as check + lease
            // cannot be expressed as an atomic operation (not that I can see anyway).
            // All considered this should still be much better than not checking lease status
            // at all.
            if (cloudBlockBlob.getProperties().getLeaseStatus() == LeaseStatus.LOCKED) {
                log.debug("Lease already acquired for container {} and zip file {}",
                    containerName, zipFilename);
                return null;
            }
            cloudBlockBlob.acquireLease(blobLeaseTimeout, null);
            return cloudBlockBlob;
        } catch (StorageException storageException) {
            if (storageException.getHttpStatusCode() == HttpStatus.CONFLICT.value()) {
                log.error(
                    "Lease already acquired for container {} and zip file {}",
                    containerName,
                    zipFilename,
                    storageException
                );
            } else {
                log.error(storageException.getMessage(), storageException);
            }
            return null;
        } catch (Exception exception) {
            log.error(
                "Failed to acquire lease on file {} in container {}",
                zipFilename,
                containerName,
                exception
            );

            return null;
        }
    }

    private ZipFileProcessor processZipFileContent(
        ZipInputStream zis,
        String zipFilename,
        String containerName
    ) {
        try {
            ZipFileProcessor zipFileProcessor = new ZipFileProcessor(); // todo: inject
            ZipVerifiers.ZipStreamWithSignature zipWithSignature =
                ZipVerifiers.ZipStreamWithSignature.fromKeyfile(zis, publicKeyDerFilename, zipFilename, containerName);
            zipFileProcessor.process(zipWithSignature, ZipVerifiers.getPreprocessor(signatureAlg));

            Envelope envelope = envelopeProcessor.parseEnvelope(zipFileProcessor.getMetadata(), zipFilename);
            envelope.setContainer(containerName);

            EnvelopeProcessor.assertEnvelopeHasPdfs(envelope, zipFileProcessor.getPdfs());
            envelopeProcessor.assertDidNotFailToUploadBefore(envelope);

            zipFileProcessor.setEnvelope(envelopeProcessor.saveEnvelope(envelope));

            return zipFileProcessor;
        } catch (EventRelatedThrowable ex) {
            registerEvent(ex);
            log.error(ex.getMessage(), ex);
            return null;
        } catch (Exception ex) {
            registerEvent(Event.DOC_FAILURE, containerName, zipFilename, ex.getMessage());
            log.error(ex.getMessage(), ex);
            return null;
        }
    }

    private boolean isReadyToBeProcessed(CloudBlockBlob blob) {
        java.util.Date cutoff = Date.from(Instant.now().minus(this.blobProcessingDelayInMinutes, ChronoUnit.MINUTES));
        return blob.getProperties().getLastModified().before(cutoff);
    }
}
