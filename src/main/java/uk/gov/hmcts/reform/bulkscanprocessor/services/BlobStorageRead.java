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
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class BlobStorageRead {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlobStorageRead.class);

    private final CloudBlobClient cloudBlobClient;

    @Autowired
    public BlobStorageRead(CloudBlobClient cloudBlobClient) {
        this.cloudBlobClient = cloudBlobClient;
    }

    public void readBlobs() {
        cloudBlobClient.listContainers().forEach(cloudBlobContainer -> {
            try {
                CloudBlobContainer container = cloudBlobClient.getContainerReference(cloudBlobContainer.getName());
                readBlobsFromContainer(container);
            } catch (URISyntaxException e) {
                LOGGER.warn("Invalid URL", e);
            } catch (StorageException e) {
                LOGGER.warn("Could not obtain container reference", e);
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
                try {
                    BlobInputStream blobInputStream = container.getBlockBlobReference(cloudBlob.getName())
                        .openInputStream();

                    return processZipFile(blobInputStream);
                } catch (StorageException e) {
                    LOGGER.warn("Could not download data", e);
                } catch (URISyntaxException e) {
                    LOGGER.warn("Invalid URL", e);
                }
                return null;
            })
            .filter(Objects::nonNull)
            .forEach((List<PDF> pdfs) -> {
                // TODO: do something with the PDFs
            });
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
            LOGGER.warn("Could not open zip file", e);
        }
        return pdfs;
    }
}
