package uk.gov.hmcts.reform.bulkscanning.info;

import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import org.springframework.boot.actuate.info.Info.Builder;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Component
public class BlobContainerInfoContributor implements InfoContributor {

    private final CloudBlobClient client;

    public BlobContainerInfoContributor(CloudBlobClient client) {
        this.client = client;
    }

    @Override
    public void contribute(Builder builder) {
        builder.withDetail(
            "containers",
            StreamSupport.stream(client.listContainers().spliterator(), false)
                .map(CloudBlobContainer::getName)
                .collect(Collectors.toList())
        );
    }
}
