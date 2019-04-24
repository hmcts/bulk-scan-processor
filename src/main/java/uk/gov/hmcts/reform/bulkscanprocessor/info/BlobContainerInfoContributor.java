package uk.gov.hmcts.reform.bulkscanprocessor.info;

import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import org.springframework.boot.actuate.info.Info.Builder;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.config.Profiles;

import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Component
@Profile(Profiles.NOT_STORAGE_STUB)
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
