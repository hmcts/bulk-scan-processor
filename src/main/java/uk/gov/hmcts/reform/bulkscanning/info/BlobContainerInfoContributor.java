package uk.gov.hmcts.reform.bulkscanning.info;

import com.google.common.collect.ImmutableMap;

import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import org.springframework.boot.actuate.info.Info.Builder;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

@Component
public class BlobContainerInfoContributor implements InfoContributor {

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
            return false;
        }
    }
}
