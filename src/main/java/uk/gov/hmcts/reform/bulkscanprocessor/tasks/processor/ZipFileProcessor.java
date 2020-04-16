package uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.NonPdfFileFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.output.Pdf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.google.common.io.ByteStreams.toByteArray;
import static uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.ZipVerifiers.ZipStreamWithSignature;

@Component
public class ZipFileProcessor {

    private static final Logger log = LoggerFactory.getLogger(ZipFileProcessor.class);

    private final String publicKeyDerFilename;
    private final String signatureAlg;

    public ZipFileProcessor(
        @Value("${storage.public_key_der_file}") String publicKeyDerFilename,
        @Value("${storage.signature_algorithm}") String signatureAlg
    ) {
        this.publicKeyDerFilename = publicKeyDerFilename;
        this.signatureAlg = signatureAlg;
    }

    public ZipFileProcessingResult process(
        ZipInputStream zis,
        String containerName,
        String zipFileName
    ) throws IOException {
        ZipStreamWithSignature signedZip = ZipStreamWithSignature.fromKeyfile(
            zis, publicKeyDerFilename, zipFileName, containerName
        );
        ZipInputStream verifiedZis = ZipVerifiers
            .getPreprocessor(signatureAlg)
            .apply(signedZip);
        ZipEntry zipEntry;

        List<Pdf> pdfs = new ArrayList<>();
        byte[] metadata = null;

        while ((zipEntry = verifiedZis.getNextEntry()) != null) {
            switch (FilenameUtils.getExtension(zipEntry.getName())) {
                case "json":
                    metadata = toByteArray(verifiedZis);

                    break;
                case "pdf":
                    pdfs.add(new Pdf(zipEntry.getName(), toByteArray(verifiedZis)));

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
