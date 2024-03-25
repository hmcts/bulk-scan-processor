package uk.gov.hmcts.reform.bulkscanprocessor.model.mapper;

import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.NonScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Payment;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputDocumentType;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputEnvelope;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputNonScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputOcrData;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputPayment;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.DocumentSubtype;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.DocumentType;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.OcrData;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.OcrDataField;
import uk.gov.hmcts.reform.bulkscanprocessor.validation.model.OcrValidationWarnings;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

/**
 * Maps input envelope to database envelope.
 */
public class EnvelopeMapper {

    // Maps metadata file document type to target ccd subtype.
    private static final Map<InputDocumentType, String> subtypeMapping =
        Map.of(
            InputDocumentType.SSCS1, DocumentSubtype.SSCS1,
            InputDocumentType.COVERSHEET, DocumentSubtype.COVERSHEET
        );

    /**
     * Maps input envelope to database envelope.
     */
    private EnvelopeMapper() {
        // utility class
    }

    /**
     * Maps input envelope to database envelope.
     * @param envelope input envelope
     * @param containerName name of the container
     * @param ocrValidationWarnings OCR validation warnings
     * @return database envelope
     */
    public static Envelope toDbEnvelope(
        InputEnvelope envelope,
        String containerName,
        Optional<OcrValidationWarnings> ocrValidationWarnings
    ) {
        return new Envelope(
            envelope.poBox,
            envelope.jurisdiction,
            envelope.deliveryDate,
            envelope.openingDate,
            envelope.zipFileCreateddate,
            envelope.zipFileName,
            envelope.caseNumber,
            envelope.previousServiceCaseReference,
            envelope.classification,
            toDbScannableItems(envelope.scannableItems, ocrValidationWarnings),
            toDbPayments(envelope.payments),
            toDbNonScannableItems(envelope.nonScannableItems),
            containerName,
            envelope.rescanFor
        );
    }

    /**
     * Scannable items to database.
     * @param scannableItems input scannable items
     * @param ocrValidationWarnings OCR validation warnings
     * @return database scannable items
     */
    private static List<ScannableItem> toDbScannableItems(
        List<InputScannableItem> scannableItems,
        Optional<OcrValidationWarnings> ocrValidationWarnings
    ) {
        if (scannableItems != null) {
            return scannableItems
                .stream()
                .map(item ->
                    toDbScannableItem(
                        item,
                        ocrValidationWarningsForItem(ocrValidationWarnings, item)
                    ))
                .collect(toList());
        } else {
            return emptyList();
        }
    }

    /**
     * Returns OCR validation warnings for the given scannable item.
     * @param ocrValidationWarnings OCR validation warnings
     * @param scannableItem scannable item
     * @return OCR validation warnings for the given scannable item
     */
    private static String[] ocrValidationWarningsForItem(
        Optional<OcrValidationWarnings> ocrValidationWarnings,
        InputScannableItem scannableItem
    ) {
        return ocrValidationWarnings
            .filter(w -> Objects.equals(w.documentControlNumber, scannableItem.documentControlNumber))
            .map(w -> w.warnings.toArray(new String[0]))
            .orElseGet(() -> new String[0]);
    }

    /**
     * Scannable item to database.
     * @param scannableItem input scannable item
     * @param ocrValidationWarnings OCR validation warnings
     * @return database scannable item
     */
    public static ScannableItem toDbScannableItem(
        InputScannableItem scannableItem,
        String[] ocrValidationWarnings
    ) {
        return new ScannableItem(
            scannableItem.documentControlNumber,
            scannableItem.scanningDate,
            scannableItem.ocrAccuracy,
            scannableItem.manualIntervention,
            scannableItem.nextAction,
            scannableItem.nextActionDate,
            mapOcrData(scannableItem.ocrData),
            scannableItem.fileName,
            scannableItem.notes,
            mapDocumentType(scannableItem.documentType),
            extractDocumentSubtype(scannableItem.documentType, scannableItem.documentSubtype),
            ocrValidationWarnings
        );
    }

    /**
     * Maps input OCR data to database OCR data.
     * @param inputOcrData input OCR data
     * @return database OCR data
     */
    private static OcrData mapOcrData(InputOcrData inputOcrData) {
        return inputOcrData == null ? null : new OcrData(
            inputOcrData
                .getFields()
                .stream()
                .map(field -> new OcrDataField(field.name, field.value))
                .collect(toList())
        );
    }

    /**
     * Maps input document type to database document type.
     * @param inputDocumentType input document type
     * @return database document type
     */
    private static DocumentType mapDocumentType(InputDocumentType inputDocumentType) {
        switch (inputDocumentType) {
            case CHERISHED:
                return DocumentType.CHERISHED;
            case COVERSHEET:
                return DocumentType.COVERSHEET;
            case FORM:
            case SSCS1:
                return DocumentType.FORM;
            case SUPPORTING_DOCUMENTS:
                return DocumentType.SUPPORTING_DOCUMENTS;
            case WILL:
                return DocumentType.WILL;
            case FORENSIC_SHEETS:
                return DocumentType.FORENSIC_SHEETS;
            case IHT:
                return DocumentType.IHT;
            case PPS_LEGAL_STATEMENT:
                return DocumentType.PPS_LEGAL_STATEMENT;
            case PPS_LEGAL_STATEMENT_WITHOUT_APOSTROPHE:
                return DocumentType.PPS_LEGAL_STATEMENT;
            default:
                return DocumentType.OTHER;
        }
    }

    /**
     * Extracts document subtype from input document type and subtype.
     * @param inputDocumentType input document type
     * @param inputDocumentSubtype input document subtype
     * @return document subtype
     */
    private static String extractDocumentSubtype(InputDocumentType inputDocumentType, String inputDocumentSubtype) {
        switch (inputDocumentType) {
            case SSCS1:
                return subtypeMapping.get(inputDocumentType);
            default:
                return inputDocumentSubtype;
        }
    }

    /**
     * Payments to database.
     * @param payments input payments
     * @return database payments
     */
    private static List<Payment> toDbPayments(List<InputPayment> payments) {
        if (payments != null) {
            return payments
                .stream()
                .map(payment -> new Payment(payment.documentControlNumber))
                .collect(toList());
        } else {
            return emptyList();
        }
    }

    /**
     * Non-scannable items to database.
     * @param nonScannableItems input non-scannable items
     * @return database non-scannable items
     */
    private static List<NonScannableItem> toDbNonScannableItems(List<InputNonScannableItem> nonScannableItems) {
        if (nonScannableItems != null) {
            return nonScannableItems
                .stream()
                .map(EnvelopeMapper::toDbNonScannableItem)
                .collect(toList());
        } else {
            return emptyList();
        }
    }

    /**
     * Non-scannable item to database.
     * @param nonScannableItem input non-scannable item
     * @return database non-scannable item
     */
    private static NonScannableItem toDbNonScannableItem(InputNonScannableItem nonScannableItem) {
        return new NonScannableItem(
            nonScannableItem.documentControlNumber,
            nonScannableItem.itemType,
            nonScannableItem.notes
        );
    }
}
