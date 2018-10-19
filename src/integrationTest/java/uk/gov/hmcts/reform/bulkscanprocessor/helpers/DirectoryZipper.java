package uk.gov.hmcts.reform.bulkscanprocessor.helpers;

import com.google.common.io.Files;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.ZipVerifiers;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.google.common.io.Resources.getResource;
import static com.google.common.io.Resources.toByteArray;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.reform.bulkscanprocessor.helpers.SigningHelper.signWithSha256Rsa;

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
     * Zips files from given directory, generates a signature for the resulting archive and then zips both file.
     */
    public static byte[] zipAndSignDir(String dirName, String signingKeyName) throws Exception {

        byte[] innerZip = zipDir(dirName);
        byte[] signature = signWithSha256Rsa(innerZip, toByteArray(getResource(signingKeyName)));

        return zipItems(
            asList(
                new ZipItem(ZipVerifiers.DOCUMENTS_ZIP, innerZip),
                new ZipItem(ZipVerifiers.SIGNATURE_SIG, signature)
            )
        );
    }

    private static byte[] zipItems(List<ZipItem> items) throws IOException {
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

    private static class ZipItem {
        final String name;
        final byte[] content;

        ZipItem(String name, byte[] content) {
            this.name = name;
            this.content = content;
        }
    }

    private DirectoryZipper() {
        // util class
    }
}
