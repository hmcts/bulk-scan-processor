package uk.gov.hmcts.reform.bulkscanprocessor.info;

import com.microsoft.azure.storage.blob.models.ContainerItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.info.Info.Builder;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.util.AzureStorageHelper;

import static java.util.stream.Collectors.toList;

@Component
public class BlobContainerInfoContributor implements InfoContributor {

    private final AzureStorageHelper client;

    @Autowired
    public BlobContainerInfoContributor(AzureStorageHelper client) {
        this.client = client;
    }

    @Override
    public void contribute(Builder builder) {
        // The asynchronous requests require we use recursion to continue our listing.
        builder.withDetail(
            "containers", client
                .listContainers()
                .blockingGet()
                .body()
                .containerItems()
                .stream()
                .map(ContainerItem::name)
                .collect(toList())
        );
    }
}
