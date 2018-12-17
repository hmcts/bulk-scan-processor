package uk.gov.hmcts.reform.bulkscanprocessor.model.mapper;

import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.NonScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Payment;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItem;
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

    public static Envelope toDbEnvelope(InputEnvelope envelope, String containerName) {
        return new Envelope(
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

    private static List<ScannableItem> toDbScannableItems(List<InputScannableItem> scannableItems) {
        if (scannableItems != null) {
            return scannableItems
                .stream()
                .map(EnvelopeMapper::toDbScannableItem)
                .collect(toList());
        } else {
            return emptyList();
        }
    }

    private static ScannableItem toDbScannableItem(InputScannableItem scannableItem) {
        return new ScannableItem(
            scannableItem.documentControlNumber,
            scannableItem.scanningDate,
            scannableItem.ocrAccuracy,
            scannableItem.manualIntervention,
            scannableItem.nextAction,
            scannableItem.nextActionDate,
            scannableItem.ocrData,
            scannableItem.fileName,
            scannableItem.notes,
            scannableItem.documentType.toLowerCase()
        );
    }

    private static List<Payment> toDbPayments(List<InputPayment> payments) {
        if (payments != null) {
            return payments
                .stream()
                .map(EnvelopeMapper::toDbPayment)
                .collect(toList());
        } else {
            return emptyList();
        }
    }

    private static Payment toDbPayment(InputPayment payment) {
        return new Payment(
            payment.documentControlNumber,
            payment.method,
            payment.amount,
            payment.currency,
            payment.paymentInstrumentNumber,
            payment.sortCode,
            payment.accountNumber
        );
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
