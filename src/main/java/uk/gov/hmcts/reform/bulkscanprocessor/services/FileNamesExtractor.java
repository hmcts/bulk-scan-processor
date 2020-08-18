package uk.gov.hmcts.reform.bulkscanprocessor.services;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobItem;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

import static java.util.stream.Collectors.toList;

public final class FileNamesExtractor {
    private static final Logger log = LoggerFactory.getLogger(FileNamesExtractor.class);

    private FileNamesExtractor() {
        // Utility class
    }

    public static List<String> getShuffledZipFileNames(BlobContainerClient container) {
        // Randomise iteration order to minimise lease acquire contention
        // For this purpose it's more efficient to have a collection that
        // implements RandomAccess (e.g. ArrayList)
        List<String> zipFilenames = container
            .listBlobs()
            .stream()
            .map(BlobItem::getName)
            .filter(FileNamesExtractor::isNotEmptyName)
            .collect(toList());

        Collections.shuffle(zipFilenames);
        return zipFilenames;
    }

    private static boolean isNotEmptyName(String fileName) {
        if (Strings.isNullOrEmpty(fileName)) {
            log.error("Filename name is empty or null.");
            return false;
        }
        return true;
    }
}
