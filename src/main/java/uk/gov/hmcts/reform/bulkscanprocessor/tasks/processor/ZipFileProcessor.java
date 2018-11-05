package uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.NonPdfFileFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.output.Pdf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.google.common.io.ByteStreams.toByteArray;

public class ZipFileProcessor {

    private static final Logger log = LoggerFactory.getLogger(ZipFileProcessor.class);

    // TODO: remove
    private Envelope envelope;

    private List<Pdf> pdfs = new ArrayList<>();

    private byte[] metadata;

    public ZipFileProcessor() {
    }

    // TODO: make it return processing result instead of setting fields on itself
    public void process(
        ZipVerifiers.ZipStreamWithSignature signedZip,
        ZipVerifiers.ZipPreprocessor preprocessor
    ) throws IOException {
        ZipInputStream zis = preprocessor.apply(signedZip);
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
                    throw new NonPdfFileFoundException(signedZip.container, signedZip.zipFileName, zipEntry.getName());
            }
        }

        log.info("PDFs found in {}: {}", signedZip.zipFileName, pdfs.size());
    }

    public byte[] getMetadata() {
        return metadata;
    }

    public List<Pdf> getPdfs() {
        return pdfs;
    }

    public Envelope getEnvelope() {
        return envelope;
    }

    public void setEnvelope(Envelope envelope) {
        this.envelope = envelope;
    }
}
