package uk.gov.hmcts.reform.bulkscanprocessor.scheduler;

import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.BlobInputStream;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.ListBlobItem;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.MetadataNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.NonPdfFileFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.DocumentManagementService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.output.Pdf;
import uk.gov.hmcts.reform.bulkscanprocessor.util.EntityParser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.google.common.io.ByteStreams.toByteArray;

/**
 * This class is a scheduler which runs as per configured interval.
 * It will read all the blobs from Azure Blob storage and will do below things
 * 1. Reads Blob from container.
 * 2. Extract Zip file(Blob)
 * 3. Transform metadata json to DB entities.
 * 4. Save PDF files in document storage.
 * 5. Update status and doc urls in DB.
 */
@Component
@ConditionalOnProperty(value = "scheduling.enabled", matchIfMissing = true)
public class BlobProcessor {

    private static final Logger log = LoggerFactory.getLogger(BlobProcessor.class);

    private final CloudBlobClient cloudBlobClient;
    private final EnvelopeRepository envelopeRepository;
    private final DocumentManagementService documentManagementService;

    @Autowired
    public BlobProcessor(
        CloudBlobClient cloudBlobClient,
        EnvelopeRepository envelopeRepository,
        DocumentManagementService documentManagementService
    ) {
        this.cloudBlobClient = cloudBlobClient;
        this.envelopeRepository = envelopeRepository;
        this.documentManagementService = documentManagementService;
    }

    @Scheduled(fixedDelayString = "${scan.delay}")
    public void processBlobs() {
        cloudBlobClient.listContainers()
            .forEach(cloudBlobContainer ->
                processBlobsPerContainer(cloudBlobContainer.getName())
            );
    }

    private void processBlobsPerContainer(String containerName) {
        log.info("Processing blobs for container {} ", containerName);

        try {
            processZipFiles(cloudBlobClient.getContainerReference(containerName));
        } catch (URISyntaxException | StorageException e) {
            log.error("Exception occurred while retrieving container reference for " + containerName, e);
            // TODO Raise an event in DB or track in appinsights if this happens.
        }
    }

    private void processZipFiles(CloudBlobContainer container) {
        for (ListBlobItem blobItem : container.listBlobs()) {
            List<Pdf> pdfFiles = new ArrayList<>();
            String zipFilename = FilenameUtils.getName(blobItem.getUri().toString());
            byte[] metadataStream = null;

            try {
                BlobInputStream blobInputStream =
                    container.getBlockBlobReference(zipFilename).openInputStream();

                //Zip file will include metadata.json and collection of pdf documents
                try (ZipInputStream zis = new ZipInputStream(blobInputStream)) {
                    ZipEntry zipEntry;
                    while ((zipEntry = zis.getNextEntry()) != null) {
                        switch (FilenameUtils.getExtension(zipEntry.getName())) {
                            case "json":
                                metadataStream = toByteArray(zis);
                                break;
                            case "pdf":
                                Pdf pdf = new Pdf(zipEntry.getName(), toByteArray(zis));
                                pdfFiles.add(pdf);
                                break;
                            default:
                                throw new NonPdfFileFoundException("Zip file contains non pdf documents");
                        }
                    }

                    processMetaFile(metadataStream);

                    processPdfFiles(pdfFiles);
                }
            } catch (Exception e) {
                //If any error occurs processing one record remaining records should be continued processing
                log.error("Exception occurred while processing zip file " + zipFilename, e);
                // TODO A record in Database needs to created with appropriate status.
            }
        }
    }

    private void processPdfFiles(List<Pdf> pdfs) {
        //TODO Save in document storage and update DB with urls
        documentManagementService.uploadDocuments(pdfs);
    }

    private void processMetaFile(byte[] metadataStream) throws IOException {
        if (Objects.isNull(metadataStream)) {
            throw new MetadataNotFoundException("No metadata file found in the zip file");
        }
        //TODO Perform json schema validation for the metadata file
        InputStream inputStream = new ByteArrayInputStream(metadataStream);
        Envelope envelope = EntityParser.parseEnvelopeMetadata(inputStream);

        envelopeRepository.save(envelope);

        log.info("Envelope for jurisdiction {} and zip file name {} successfully saved in database.",
            envelope.getJurisdiction(),
            envelope.getZipFileName()
        );
    }
}
