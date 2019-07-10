package uk.gov.hmcts.reform.bulkscanprocessor.validation;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import uk.gov.hmcts.reform.bulkscanprocessor.config.ContainerMappings;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ContainerJurisdictionPoBoxMismatchException;
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
import java.util.stream.Stream;

import static java.util.Objects.isNull;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

public final class EnvelopeValidator {

    private static final InputDocumentType defaultOcrDocymentType = InputDocumentType.FORM;
    private static final Map<String, InputDocumentType> ocrDocumentTypePerJurisdiction =
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

        if (envelope.classification == Classification.NEW_APPLICATION) {

            List<InputDocumentType> typesThatShouldHaveOcrData =
                Stream.of(
                    defaultOcrDocymentType,
                    ocrDocumentTypePerJurisdiction.get(envelope.jurisdiction)
                ).filter(Objects::nonNull)
                .collect(toList());

            List<InputScannableItem> docsThatShouldHaveOcr = envelope
                .scannableItems
                .stream()
                .filter(doc -> typesThatShouldHaveOcrData.contains(doc.documentType))
                .collect(toList());

            if (docsThatShouldHaveOcr.isEmpty()) {
                String types = typesThatShouldHaveOcrData.stream().map(t -> t.toString()).collect(joining(", "));
                throw new OcrDataNotFoundException("No documents of type " + types + " found");
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
        List<String> problems = new ArrayList<>();

        List<String> duplicateFileNames =
            envelope
                .scannableItems
                .stream()
                .map(it -> it.fileName)
                .collect(groupingBy(it -> it, counting()))
                .entrySet()
                .stream()
                .filter(it -> it.getValue() > 1)
                .map(it -> it.getKey())
                .collect(toList());
        
        if (!duplicateFileNames.isEmpty()) {
            problems.add("Duplicate scanned items file names: " + String.join(", ", duplicateFileNames));
        }

        Set<String> scannedFileNames = envelope
            .scannableItems
            .stream()
            .map(item -> item.fileName)
            .collect(toSet());

        Set<String> pdfFileNames = pdfs
            .stream()
            .map(Pdf::getFilename)
            .collect(toSet());

        Set<String> missingActualPdfFiles = Sets.difference(scannedFileNames, pdfFileNames);
        Set<String> notDeclaredPdfs = Sets.difference(pdfFileNames, scannedFileNames);


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


    /**
     * Assert container is configured for the jurisdiction and po box.
     * Throws exception otherwise.
     *
     * @param mappings      container mappings with jurisdiction and PoBox
     * @param envelope      to assert against
     * @param containerName container from which envelope was retrieved
     */
    public static void assertContainerMatchesJurisdictionAndPoBox(
        List<ContainerMappings.Mapping> mappings,
        InputEnvelope envelope,
        String containerName
    ) {
        boolean isMatched = mappings.stream()
            .anyMatch(mapping ->
                mapping.getContainer().equalsIgnoreCase(containerName)
                    && mapping.getJurisdiction().equalsIgnoreCase(envelope.jurisdiction)
                    && mapping.getPoBox().equalsIgnoreCase(envelope.poBox)
            );

        if (!isMatched) {
            throw new ContainerJurisdictionPoBoxMismatchException(
                String.format(
                    "Container, PO Box and jurisdiction mismatch. Jurisdiction: %s, PO Box: %s, container: %s",
                    envelope.jurisdiction,
                    envelope.poBox,
                    containerName
                )
            );
        }
    }
}
