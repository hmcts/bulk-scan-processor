package uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.NonPdfFileFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.output.Pdf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.google.common.io.ByteStreams.toByteArray;

public class ZipFileProcessor {

    private static final Logger log = LoggerFactory.getLogger(ZipFileProcessor.class);

    public ZipFileProcessor() {
    }

    public ZipFileProcessingResult process(
        ZipVerifiers.ZipStreamWithSignature signedZip,
        Function<ZipVerifiers.ZipStreamWithSignature, ZipInputStream> preprocessor
    ) throws IOException {
        ZipInputStream zis = preprocessor.apply(signedZip);
        ZipEntry zipEntry;

        List<Pdf> pdfs = new ArrayList<>();
        byte[] metadata = null;

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
                    throw new NonPdfFileFoundException(signedZip.zipFileName, zipEntry.getName());
            }
        }

        log.info("PDFs found in {}: {}", signedZip.zipFileName, pdfs.size());

        return new ZipFileProcessingResult(metadata, pdfs);
    }
}
