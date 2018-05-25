package uk.gov.hmcts.reform.bulkscanning.info;

import com.google.common.collect.ImmutableMap;

import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.info.Info.Builder;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

@Component
public class BlobContainerInfoContributor implements InfoContributor {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlobContainerInfoContributor.class);
    private final CloudBlobContainer container;

    public BlobContainerInfoContributor(CloudBlobContainer container) {
        this.container = container;
    }

    @Override
    public void contribute(Builder builder) {
        builder.withDetails(ImmutableMap.of(
            "container_name", container.getName(),
            "container_exists", doesContainerExist()
        ));
    }

    private boolean doesContainerExist() {
        try {
            return container.exists();
        } catch (StorageException e) {
            LOGGER.warn("Could not check if container exists", e);
            return false;
        }
    }
}
