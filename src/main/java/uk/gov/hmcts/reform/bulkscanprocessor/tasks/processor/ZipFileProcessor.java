package uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.NonPdfFileFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.output.Pdf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.google.common.io.ByteStreams.toByteArray;

@Component
public class ZipFileProcessor {

    private static final Logger log = LoggerFactory.getLogger(ZipFileProcessor.class);

    private final ZipExtractor zipExtractor;

    public ZipFileProcessor(ZipExtractor zipExtractor) {
        this.zipExtractor = zipExtractor;
    }

    public ZipFileProcessingResult process(
        ZipInputStream zis,
        String zipFileName
    ) throws IOException {
        log.info("Processing zip file {}", zipFileName);

        ZipInputStream extractedZis = zipExtractor.extract(zis);

        log.info("Extracted zip entries from {}", zipFileName);

        ZipEntry zipEntry;

        List<Pdf> pdfs = new ArrayList<>();
        byte[] metadata = null;

        while ((zipEntry = extractedZis.getNextEntry()) != null) {
            switch (FilenameUtils.getExtension(zipEntry.getName())) {
                case "json":
                    metadata = toByteArray(extractedZis);

                    break;
                case "pdf":
                    pdfs.add(new Pdf(zipEntry.getName(), toByteArray(extractedZis)));

                    break;
                default:
                    // contract breakage
                    throw new NonPdfFileFoundException(zipFileName, zipEntry.getName());
            }
        }

        log.info("PDFs found in {}: {}", zipFileName, pdfs.size());

        return new ZipFileProcessingResult(metadata, pdfs);
    }
}
