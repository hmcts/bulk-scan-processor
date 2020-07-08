package uk.gov.hmcts.reform.bulkscanprocessor.services;

import com.google.common.base.Strings;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@ConditionalOnProperty(value = "scheduling.task.scan.enabled", matchIfMissing = true)
public class FileNamesExtractor {
    private static final Logger log = LoggerFactory.getLogger(FileNamesExtractor.class);

    private FileNamesExtractor() {
        // Utility class
    }

    public static List<String> getZipFileNamesFromContainer(CloudBlobContainer container) {
        // Randomise iteration order to minimise lease acquire contention
        // For this purpose it's more efficient to have a collection that
        // implements RandomAccess (e.g. ArrayList)
        List<String> zipFilenames = new ArrayList<>();
        container
            .listBlobs()
            .forEach(b -> {
                String fileName = FilenameUtils.getName(b.getUri().toString());
                if (Strings.isNullOrEmpty(fileName)) {
                    log.error("Cannot extract filename from list blob item. URI: {}", b.getUri());
                } else {
                    zipFilenames.add(fileName);
                }
            });
        Collections.shuffle(zipFilenames);
        return zipFilenames;
    }
}
