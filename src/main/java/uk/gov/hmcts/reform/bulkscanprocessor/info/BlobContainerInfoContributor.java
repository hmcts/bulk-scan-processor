package uk.gov.hmcts.reform.bulkscanprocessor.info;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobContainerItem;
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

    private final BlobServiceClient client;

    public BlobContainerInfoContributor(BlobServiceClient client) {
        this.client = client;
    }

    @Override
    public void contribute(Builder builder) {
        builder.withDetail(
            "containers",
            StreamSupport.stream(client.listBlobContainers().spliterator(), false)
                .map(BlobContainerItem::getName)
                .collect(Collectors.toList())
        );
    }
}
