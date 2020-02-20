package uk.gov.hmcts.reform.bulkscanprocessor.validation;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import uk.gov.hmcts.reform.bulkscanprocessor.config.ContainerMappings;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ContainerJurisdictionPoBoxMismatchException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.DisallowedDocumentTypesException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.DuplicateDocumentControlNumbersInEnvelopeException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.FileNameIrregularitiesException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.OcrDataNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.PaymentsDisabledException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ServiceDisabledException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ZipNameNotMatchingMetaDataException;
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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

public final class EnvelopeValidator {

    private static final InputDocumentType defaultOcrDocumentType = InputDocumentType.FORM;
    private static final Map<String, InputDocumentType> ocrDocumentTypePerJurisdiction =
        ImmutableMap.of(
            "SSCS", InputDocumentType.SSCS1
        );

    private static final Map<Classification, List<InputDocumentType>> disallowedDocumentTypes =
        ImmutableMap.of(
            Classification.EXCEPTION, emptyList(),
            Classification.NEW_APPLICATION, emptyList(),
            Classification.SUPPLEMENTARY_EVIDENCE, asList(InputDocumentType.FORM, InputDocumentType.SSCS1),
            Classification.SUPPLEMENTARY_EVIDENCE_WITH_OCR, emptyList()
        );

    private EnvelopeValidator() {
        // util class
    }

    /**
     * Assert envelope contains only scannable items of types that are allowed for the envelope's classification.
     * Otherwise, throws an exception.
     *
     * @param envelope to assert against
     */
    public static void assertEnvelopeContainsDocsOfAllowedTypesOnly(InputEnvelope envelope) {
        List<String> disallowedDocTypesFound =
            envelope
                .scannableItems
                .stream()
                .filter(item -> disallowedDocumentTypes.get(envelope.classification).contains(item.documentType))
                .map(item -> item.documentType.toString())
                .collect(toList());

        if (!disallowedDocTypesFound.isEmpty()) {
            String errorMessage = String.format(
                "Envelope contains scannable item(s) of types that are not allowed for classification '%s': [%s]",
                envelope.classification,
                StringUtils.join(disallowedDocTypesFound, ", ")
            );

            throw new DisallowedDocumentTypesException(errorMessage);
        }
    }

    /**
     * Assert scannable items contain ocr data
     * when envelope classification is NEW_APPLICATION or SUPPLEMENTARY_EVIDENCE_WITH_OCR
     * Throws exception otherwise.
     *
     * @param envelope to assert against
     */
    public static void assertEnvelopeContainsOcrDataIfRequired(InputEnvelope envelope) {

        if (envelope.classification == Classification.NEW_APPLICATION
            || envelope.classification == Classification.SUPPLEMENTARY_EVIDENCE_WITH_OCR) {

            List<InputDocumentType> typesThatShouldHaveOcrData =
                Stream.of(
                    defaultOcrDocumentType,
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
            getDuplicates(envelope.scannableItems.stream().map(it -> it.fileName).collect(toList()));

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

    public static void assertDocumentControlNumbersAreUnique(InputEnvelope envelope) {
        List<String> dcns = envelope.scannableItems.stream().map(it -> it.documentControlNumber).collect(toList());
        List<String> duplicateDcns = getDuplicates(dcns);
        if (!duplicateDcns.isEmpty()) {
            throw new DuplicateDocumentControlNumbersInEnvelopeException(
                "Duplicate DCNs in envelope: " + String.join(", ", duplicateDcns)
            );
        }
    }

    public static void assertZipFilenameMatchesWithMetadata(InputEnvelope envelope, String zipFileName) {
        if (!envelope.zipFileName.equals(zipFileName)) {
            throw new ZipNameNotMatchingMetaDataException(
                "Name of the uploaded zip file does not match with field \"zip_file_name\" in the metadata"
            );
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

    public static void assertPaymentsEnabledForContainerIfPaymentsArePresent(
        InputEnvelope envelope,
        boolean paymentsEnabled,
        List<ContainerMappings.Mapping> mappings
    ) {
        if (isNotEmpty(envelope.payments)
            && (!paymentsEnabled || !isPaymentsEnabledForContainer(mappings, envelope))
        ) {
            throw new PaymentsDisabledException(
                String.format(
                    "Envelope contains payment(s) that are not allowed for jurisdiction '%s', poBox: '%s'",
                    envelope.jurisdiction,
                    envelope.poBox
                )
            );
        }
    }

    public static void assertServiceEnabled(
        InputEnvelope envelope,
        List<ContainerMappings.Mapping> mappings
    ) {
        Boolean isServiceEnabled = mappings
            .stream()
            .filter(mapping ->
                mapping.getJurisdiction().equalsIgnoreCase(envelope.jurisdiction)
                    && mapping.getPoBox().equalsIgnoreCase(envelope.poBox)
            )
            .findFirst()
            .map(ContainerMappings.Mapping::isEnabled)
            .orElse(false);

        if (!isServiceEnabled) {
            throw new ServiceDisabledException(
                String.format(
                    "Envelope contains service that is not enabled. Jurisdiction: '%s' POBox: '%s'",
                    envelope.jurisdiction,
                    envelope.poBox
                )
            );
        }
    }

    private static boolean isPaymentsEnabledForContainer(
        List<ContainerMappings.Mapping> mappings,
        InputEnvelope envelope
    ) {
        return mappings
            .stream()
            .filter(mapping ->
                mapping.getJurisdiction().equalsIgnoreCase(envelope.jurisdiction)
                    && mapping.getPoBox().equalsIgnoreCase(envelope.poBox)
            )
            .findFirst()
            .map(ContainerMappings.Mapping::isPaymentsEnabled)
            .orElse(false);
    }

    private static List<String> getDuplicates(List<String> collection) {
        return collection
            .stream()
            .collect(groupingBy(it -> it, counting()))
            .entrySet()
            .stream()
            .filter(entry -> entry.getValue() > 1)
            .map(entry -> entry.getKey())
            .collect(toList());
    }
}
