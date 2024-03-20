package uk.gov.hmcts.reform.bulkscanprocessor.info;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobContainerItem;
import org.springframework.boot.actuate.info.Info.Builder;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

import static java.util.stream.Collectors.toList;

/**
 * Contributes information about the blob containers to the /info endpoint.
 */
@Component
public class BlobContainerInfoContributor implements InfoContributor {

    private final BlobServiceClient client;

    /**
     * Creates a new instance of the contributor.
     * @param client the blob service client
     */
    public BlobContainerInfoContributor(BlobServiceClient client) {
        this.client = client;
    }

    /**
     * Contributes information about the blob containers to the /info endpoint.
     * @param builder the info builder
     */
    @Override
    public void contribute(Builder builder) {
        builder.withDetail(
            "containers",
            client.listBlobContainers()
                .stream()
                .map(BlobContainerItem::getName)
                .collect(toList())
        );
    }
}
