package uk.gov.hmcts.reform.bulkscanprocessor.model.mapper;

import uk.gov.hmcts.reform.bulkscanprocessor.entity.DbEnvelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.DbNonScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.DbPayment;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.DbScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputEnvelope;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputNonScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputPayment;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputScannableItem;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

public class EnvelopeMapper {

    private EnvelopeMapper() {
        // utility class
    }

    public static DbEnvelope toDbEnvelope(InputEnvelope envelope, String containerName) {
        return new DbEnvelope(
            envelope.poBox,
            envelope.jurisdiction,
            envelope.deliveryDate,
            envelope.openingDate,
            envelope.zipFileCreateddate,
            envelope.zipFileName,
            envelope.caseNumber,
            envelope.classification,
            toDbScannableItems(envelope.scannableItems),
            toDbPayments(envelope.payments),
            toDbNonScannableItems(envelope.nonScannableItems),
            containerName
        );
    }

    private static List<DbScannableItem> toDbScannableItems(List<InputScannableItem> scannableItems) {
        if (scannableItems != null) {
            return scannableItems
                .stream()
                .map(EnvelopeMapper::toDbScannableItem)
                .collect(toList());
        } else {
            return emptyList();
        }
    }

    private static DbScannableItem toDbScannableItem(InputScannableItem scannableItem) {
        return new DbScannableItem(
            scannableItem.documentControlNumber,
            scannableItem.scanningDate,
            scannableItem.ocrAccuracy,
            scannableItem.manualIntervention,
            scannableItem.nextAction,
            scannableItem.nextActionDate,
            scannableItem.ocrData,
            scannableItem.fileName,
            scannableItem.notes,
            scannableItem.documentType
        );
    }

    private static List<DbPayment> toDbPayments(List<InputPayment> payments) {
        if (payments != null) {
            return payments
                .stream()
                .map(EnvelopeMapper::toDbPayment)
                .collect(toList());
        } else {
            return emptyList();
        }
    }

    private static DbPayment toDbPayment(InputPayment payment) {
        return new DbPayment(
            payment.documentControlNumber,
            payment.method,
            payment.amount,
            payment.currency,
            payment.paymentInstrumentNumber,
            payment.sortCode,
            payment.accountNumber
        );
    }

    private static List<DbNonScannableItem> toDbNonScannableItems(List<InputNonScannableItem> nonScannableItems) {
        if (nonScannableItems != null) {
            return nonScannableItems
                .stream()
                .map(EnvelopeMapper::toDbNonScannableItem)
                .collect(toList());
        } else {
            return emptyList();
        }
    }

    private static DbNonScannableItem toDbNonScannableItem(InputNonScannableItem nonScannableItem) {
        return new DbNonScannableItem(
            nonScannableItem.documentControlNumber,
            nonScannableItem.itemType,
            nonScannableItem.notes
        );
    }
}
