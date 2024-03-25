package uk.gov.hmcts.reform.bulkscanprocessor.services;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobItem;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * Utility class to extract file names from a blob container.
 */
public final class FileNamesExtractor {
    private static final Logger log = LoggerFactory.getLogger(FileNamesExtractor.class);

    /**
     * Constructor for FileNamesExtractor.
     */
    private FileNamesExtractor() {
        // Utility class
    }

    /**
     * Returns a list of shuffled zip file names in the given container.
     * @param container the container to get the file names from
     */
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

    /**
     * Checks if the file name is not empty.
     * @param fileName the file name to check
     * @return true if the file name is not empty, false otherwise
     */
    private static boolean isNotEmptyName(String fileName) {
        if (Strings.isNullOrEmpty(fileName)) {
            log.error("Filename name is empty or null.");
            return false;
        }
        return true;
    }
}
