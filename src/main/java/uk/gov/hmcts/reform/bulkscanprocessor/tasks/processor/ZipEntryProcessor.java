package uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.NoPdfFileFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.output.Pdf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.google.common.io.ByteStreams.toByteArray;

public class ZipEntryProcessor {

    private static final Logger log = LoggerFactory.getLogger(ZipEntryProcessor.class);

    private final String containerName;

    private final String zipFileName;

    private List<Pdf> pdfs = new ArrayList<>();

    private byte[] metadata;

    public ZipEntryProcessor(String containerName, String zipFileName) {
        this.containerName = containerName;
        this.zipFileName = zipFileName;
    }

    public void process(ZipInputStream zis) throws IOException {
        ZipEntry zipEntry;

        while ((zipEntry = zis.getNextEntry()) != null) {
            switch (FilenameUtils.getExtension(zipEntry.getName())) {
                case "json":
                    metadata = toByteArray(zis);

                    break;
                case "pdf":
                    pdfs.add(new Pdf(zipEntry.getName(), toByteArray(zis)));

                    break;
                default:
                    // contract breakage
                    throw new NoPdfFileFoundException(containerName, zipFileName);
            }
        }

        log.info("PDFs found in {}: {}", zipFileName, pdfs.size());
    }

    public byte[] getMetadata() {
        return metadata;
    }

    public List<Pdf> getPdfs() {
        return pdfs;
    }
}
