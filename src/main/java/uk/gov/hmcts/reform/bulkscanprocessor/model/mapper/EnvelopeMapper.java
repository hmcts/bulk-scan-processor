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

public class EnvelopeMapper {

    // Maps metadata file document type to target ccd subtype.
    private static final Map<InputDocumentType, String> subtypeMapping =
        Map.of(
            InputDocumentType.SSCS1, DocumentSubtype.SSCS1,
            InputDocumentType.COVERSHEET, DocumentSubtype.COVERSHEET
        );

    private EnvelopeMapper() {
        // utility class
    }

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

    private static String[] ocrValidationWarningsForItem(
        Optional<OcrValidationWarnings> ocrValidationWarnings,
        InputScannableItem scannableItem
    ) {
        return ocrValidationWarnings
            .filter(w -> Objects.equals(w.documentControlNumber, scannableItem.documentControlNumber))
            .map(w -> w.warnings.toArray(new String[0]))
            .orElseGet(() -> new String[0]);
    }

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

    private static OcrData mapOcrData(InputOcrData inputOcrData) {
        return inputOcrData == null ? null : new OcrData(
            inputOcrData
                .getFields()
                .stream()
                .map(field -> new OcrDataField(field.name, field.value))
                .collect(toList())
        );
    }

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
            default:
                return DocumentType.OTHER;
        }
    }

    private static String extractDocumentSubtype(InputDocumentType inputDocumentType, String inputDocumentSubtype) {
        switch (inputDocumentType) {
            case SSCS1:
                return subtypeMapping.get(inputDocumentType);
            default:
                return inputDocumentSubtype;
        }
    }

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

    private static NonScannableItem toDbNonScannableItem(InputNonScannableItem nonScannableItem) {
        return new NonScannableItem(
            nonScannableItem.documentControlNumber,
            nonScannableItem.itemType,
            nonScannableItem.notes
        );
    }
}
