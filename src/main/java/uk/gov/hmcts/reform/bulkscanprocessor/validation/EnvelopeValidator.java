package uk.gov.hmcts.reform.bulkscanprocessor.validation;

import com.google.common.collect.Sets;
import org.apache.commons.collections4.MapUtils;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.FileNameIrregularitiesException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.OcrDataNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputDocumentType;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputEnvelope;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.output.Pdf;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class EnvelopeValidator {

    private EnvelopeValidator() {
        // util class
    }

    /**
     * Assert scannable item contains ocr data
     * when envelope classification is NEW_APPLICATION
     * and jurisdiction is SSCS
     * and document type of the scannable item is SSCS1.
     * Throws exception otherwise.
     *
     * @param envelope to assert against
     */
    public static void assertEnvelopeContainsOcrDataIfRequired(InputEnvelope envelope) {

        if (envelope.jurisdiction.equalsIgnoreCase("SSCS")
            && Classification.NEW_APPLICATION.equals(envelope.classification)) {

            boolean ocrDataExists = envelope.scannableItems
                .stream()
                .filter(item -> item.documentType.equals(InputDocumentType.SSCS1))
                .noneMatch(item -> MapUtils.isEmpty(item.ocrData));

            if (!ocrDataExists) {
                throw new OcrDataNotFoundException("No scannable items found with ocr data and document type "
                    + InputDocumentType.SSCS1);
            }
        }
    }

    /**
     * Assert given envelope has scannable items exactly matching
     * the filenames with list of pdfs acquired from zip file.
     * In case there is a mismatch an exception is thrown.
     *
     * @param envelope to assert against
     * @param pdfs     to assert against
     */
    public static void assertEnvelopeHasPdfs(InputEnvelope envelope, List<Pdf> pdfs) {
        Set<String> scannedFileNames = envelope
            .scannableItems
            .stream()
            .map(item -> item.fileName)
            .collect(Collectors.toSet());

        Set<String> pdfFileNames = pdfs
            .stream()
            .map(Pdf::getFilename)
            .collect(Collectors.toSet());

        Set<String> missingActualPdfFiles = Sets.difference(scannedFileNames, pdfFileNames);
        Set<String> notDeclaredPdfs = Sets.difference(pdfFileNames, scannedFileNames);

        List<String> problems = new ArrayList<>();

        if (!notDeclaredPdfs.isEmpty()) {
            problems.add("Not declared PDFs: " + String.join(", ", notDeclaredPdfs));
        }

        if (!missingActualPdfFiles.isEmpty()) {
            problems.add("Missing PDFs: " + String.join(", ", missingActualPdfFiles));
        }

        if (!problems.isEmpty()) {
            throw new FileNameIrregularitiesException(String.join(". ", problems));
        }
    }
}
