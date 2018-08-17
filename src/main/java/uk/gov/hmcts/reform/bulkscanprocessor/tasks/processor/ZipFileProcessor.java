package uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.FileNameIrregularitiesException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.NonPdfFileFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.output.Pdf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.google.common.io.ByteStreams.toByteArray;

public class ZipFileProcessor {

    private static final Logger log = LoggerFactory.getLogger(ZipFileProcessor.class);

    private final String containerName;

    private final String zipFileName;

    private Envelope envelope;

    private List<Pdf> pdfs = new ArrayList<>();

    private byte[] metadata;

    public ZipFileProcessor(String containerName, String zipFileName) {
        this.containerName = containerName;
        this.zipFileName = zipFileName;
    }

    public ZipFileProcessor(Envelope envelope) {
        this(envelope.getContainer(), envelope.getZipFileName());
        this.envelope = envelope;
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
                    throw new NonPdfFileFoundException(containerName, zipFileName, zipEntry.getName());
            }
        }

        log.info("PDFs found in {}: {}", zipFileName, pdfs.size());
    }

    public void assertEnvelopeHasPdfs() {
        Set<String> scannedFileNames = envelope
            .getScannableItems()
            .stream()
            .map(ScannableItem::getFileName)
            .collect(Collectors.toSet());
        Set<String> pdfFileNames = pdfs
            .stream()
            .map(Pdf::getFilename)
            .collect(Collectors.toSet());

        Collection<String> missingScannedFiles = new HashSet<>(scannedFileNames);
        missingScannedFiles.removeAll(pdfFileNames);
        Collection<String> missingPdfFiles = new HashSet<>(pdfFileNames);
        missingPdfFiles.removeAll(scannedFileNames);

        missingScannedFiles.addAll(missingPdfFiles);

        if (missingScannedFiles.size() > 0) {
            throw new FileNameIrregularitiesException(envelope, missingScannedFiles);
        }
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
