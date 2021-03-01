package uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.NonPdfFileFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.output.Pdf;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.google.common.io.ByteStreams.toByteArray;

@Component
public class ZipFileProcessor {

    private static final Logger log = LoggerFactory.getLogger(ZipFileProcessor.class);
    public static final String DOWNLOAD_PATH = "/var/tmp/download/blobs";

    public ZipFileProcessingResult process(
        ZipInputStream extractedZis,
        String zipFileName
    ) throws IOException {
        return saveToTemp(extractedZis, zipFileName);
    }

    public void deleteFolder(String zipFileName) {
        String folderPath =  DOWNLOAD_PATH + File.separator +  zipFileName;
        try {
            FileUtils.deleteDirectory(new File(folderPath));
        } catch (IOException e) {
            log.error("Folder delete unsuccessful, path: {} ", folderPath, e);
        }
    }

    public ZipFileProcessingResult getZipContentDetail(
        ZipInputStream extractedZis,
        String zipFileName
    ) throws IOException {

        ZipEntry zipEntry;

        List<Pdf> pdfs = new ArrayList<>();
        byte[] metadata = null;

        while ((zipEntry = extractedZis.getNextEntry()) != null) {
            switch (FilenameUtils.getExtension(zipEntry.getName())) {
                case "json":
                    metadata = toByteArray(extractedZis);
                    log.info(
                        "File: {}, Meta data size: {}",
                        zipFileName,
                        FileUtils.byteCountToDisplaySize(metadata.length)
                    );
                    break;
                case "pdf":
                    pdfs.add(new Pdf(zipEntry.getName(), null));
                    break;
                default:
                    // contract breakage
                    throw new NonPdfFileFoundException(zipFileName, zipEntry.getName());
            }
        }

        log.info("PDFs found in {}: {}", zipFileName, pdfs.size());

        return new ZipFileProcessingResult(metadata, pdfs);
    }

    public ZipFileProcessingResult saveToTemp(
        ZipInputStream extractedZis,
        String zipFileName
    ) throws IOException {

        ZipEntry zipEntry;

        List<Pdf> pdfs = new ArrayList<>();

        String folderPath =  DOWNLOAD_PATH + File.separator + zipFileName;

        while ((zipEntry = extractedZis.getNextEntry()) != null) {
            switch (FilenameUtils.getExtension(zipEntry.getName())) {
                case "json":
                    break;
                case "pdf":
                    String filePath =
                        folderPath + File.separator + FilenameUtils.getName(zipEntry.getName());
                    var pdfFile = new File(filePath);
                    FileUtils.copyToFile(extractedZis, pdfFile);
                    pdfs.add(new Pdf(zipEntry.getName(), pdfFile));
                    break;
                default:
                    // contract breakage
                    throw new NonPdfFileFoundException(zipFileName, zipEntry.getName());
            }
        }

        log.info("PDFs found in {}: {}, saved to {} ", zipFileName, pdfs.size(), folderPath);

        return new ZipFileProcessingResult(null, pdfs);
    }
}
