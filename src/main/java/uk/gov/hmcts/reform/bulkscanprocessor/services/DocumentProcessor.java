package uk.gov.hmcts.reform.bulkscanprocessor.services;

import com.google.common.io.ByteStreams;

import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.BlobInputStream;
import com.microsoft.azure.storage.blob.CloudBlob;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Main component that is run in intervals.
 *
 * Looks up all available containers for zip files and extracts pdf files from them.
 */
@Component
public class DocumentProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentProcessor.class);

    private final CloudBlobClient cloudBlobClient;
    private final Consumer<List<PDF>> handlePdfs;

    @Autowired
    public DocumentProcessor(CloudBlobClient cloudBlobClient, Consumer<List<PDF>> handlePdfs) {
        this.cloudBlobClient = cloudBlobClient;
        this.handlePdfs = handlePdfs;
    }

    @Scheduled(fixedDelayString = "${scan.delay}")
    public void readBlobs() {
        cloudBlobClient.listContainers().forEach(cloudBlobContainer -> {
            try {
                String containerName = cloudBlobContainer.getName();
                LOGGER.info("Processing {} container", containerName);
                CloudBlobContainer container = cloudBlobClient.getContainerReference(containerName);
                readBlobsFromContainer(container);
            } catch (URISyntaxException e) {
                LOGGER.error("Invalid URL", e);
                // TODO: track exception in AppInsights Telemetry
            } catch (StorageException e) {
                LOGGER.error("Could not obtain container reference", e);
                // TODO: track exception in AppInsights Telemetry
            }
        });
    }

    private void readBlobsFromContainer(CloudBlobContainer container) {
        StreamSupport.stream(container.listBlobs().spliterator(), false)
            .filter(CloudBlob.class::isInstance)
            .map(CloudBlob.class::cast)
            .filter(cloudBlob -> cloudBlob.getName().toLowerCase().endsWith(".zip"))
            // TODO: notify supplier about non-zip files
            .map(cloudBlob -> {
                try (BlobInputStream blobInputStream = container.getBlockBlobReference(cloudBlob.getName())
                    .openInputStream()) {
                    return processZipFile(blobInputStream);
                } catch (StorageException e) {
                    LOGGER.error("Could not download data", e);
                    // TODO: track exception in AppInsights Telemetry
                } catch (URISyntaxException e) {
                    LOGGER.error("Invalid URL", e);
                    // TODO: track exception in AppInsights Telemetry
                } catch (IOException e) {
                    LOGGER.error("Could not close data stream", e);
                    // TODO: track exception in AppInsights Telemetry
                }
                return null;
            })
            .filter(Objects::nonNull)
            .forEach(handlePdfs);
    }

    private List<PDF> processZipFile(BlobInputStream blobInputStream) {
        List<PDF> pdfs = new ArrayList<>();

        try (ZipInputStream zis = new ZipInputStream(blobInputStream)) {
            ZipEntry ze;
            while ((ze = zis.getNextEntry()) != null) {
                PDF pdf = new PDF(ze.getName(), ByteStreams.toByteArray(zis));
                pdfs.add(pdf);
            }
        } catch (IOException e) {
            LOGGER.error("Could not open zip file", e);
            // TODO: track exception in AppInsights Telemetry
        }
        return pdfs;
    }
}
