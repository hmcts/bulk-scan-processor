package uk.gov.hmcts.reform.bulkscanprocessor.validation;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ContainerJurisdictionMismatchException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.FileNameIrregularitiesException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.OcrDataNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputDocumentType;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputEnvelope;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.output.Pdf;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

public final class EnvelopeValidator {

    static final Map<String, InputDocumentType> ocrDocumentTypePerJurisdiction =
        ImmutableMap.of(
            "SSCS", InputDocumentType.SSCS1
        );

    private EnvelopeValidator() {
        // util class
    }

    /**
     * Assert scannable items contain ocr data
     * when envelope classification is NEW_APPLICATION
     * Throws exception otherwise.
     *
     * @param envelope to assert against
     */
    public static void assertEnvelopeContainsOcrDataIfRequired(InputEnvelope envelope) {

        if (Classification.NEW_APPLICATION.equals(envelope.classification)) {

            InputDocumentType typeThatShouldHaveOcrData = ocrDocumentTypePerJurisdiction.get(envelope.jurisdiction);

            if (typeThatShouldHaveOcrData == null) {
                throw new OcrDataNotFoundException(
                    "OCR document type for jurisdiction " + envelope.jurisdiction + " not configured"
                );
            }

            List<InputScannableItem> docsThatShouldHaveOcr = envelope
                .scannableItems
                .stream()
                .filter(doc -> Objects.equals(doc.documentType, typeThatShouldHaveOcrData))
                .collect(toList());

            if (docsThatShouldHaveOcr.isEmpty()) {
                throw new OcrDataNotFoundException("No documents of type " + typeThatShouldHaveOcrData + " found");
            }

            if (docsThatShouldHaveOcr
                .stream()
                .allMatch(
                    doc -> isNull(doc.ocrData) || isEmpty(doc.ocrData.getFields())
                )
            ) {
                throw new OcrDataNotFoundException("Missing OCR data");
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

    public static void assertContainerMatchesJurisdiction(InputEnvelope envelope, String containerName) {
        if (!StringUtils.equalsIgnoreCase(envelope.jurisdiction, containerName)) {
            throw new ContainerJurisdictionMismatchException(
                String.format(
                    "Container and jurisdiction mismatch. Jurisdiction: %s, container: %s",
                    envelope.jurisdiction,
                    containerName
                )
            );
        }
    }
}
