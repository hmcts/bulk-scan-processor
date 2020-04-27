package uk.gov.hmcts.reform.bulkscanprocessor.helper;

import com.google.common.io.Files;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.ZipExtractor;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.google.common.io.Resources.getResource;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

public final class DirectoryZipper {

    /**
     * Zips files from given directory. Files in resulting archive are NOT wrapped in a directory.
     */
    public static byte[] zipDir(String dirName) throws IOException {

        // .listFiles() does not guarantee any order.
        // Files need to be sorted as currently mocks rely on files order.
        // TODO: improve mocks in tests so that order does not matter
        return zipItems(
            Stream.of(new File(getResource(dirName).getPath()).listFiles())
                .sorted()
                .map(f -> new ZipItem(f.getName(), getFileBytes(f)))
                .collect(toList())
        );
    }

    /**
     * Zips files from given directory and then zips it in wrapping zip.
     */
    public static byte[] zipDirAndWrap(String dirName) throws Exception {

        byte[] innerZip = zipDir(dirName);

        return zipItems(
            asList(
                new ZipItem(ZipExtractor.DOCUMENTS_ZIP, innerZip)
            )
        );
    }

    public static byte[] zipItems(List<ZipItem> items) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(outputStream)) {

            for (ZipItem item : items) {
                zos.putNextEntry(new ZipEntry(item.name));
                zos.write(item.content);
                zos.closeEntry();
            }
        }

        return outputStream.toByteArray();
    }

    private static byte[] getFileBytes(File file) {
        // wrap in runtime exception so that this can be used in .map()
        try {
            return Files.toByteArray(file);
        } catch (IOException exc) {
            throw new RuntimeException(exc);
        }
    }

    public static class ZipItem {
        final String name;
        final byte[] content;

        public ZipItem(String name, byte[] content) {
            this.name = name;
            this.content = content;
        }
    }

    private DirectoryZipper() {
        // util class
    }
}
