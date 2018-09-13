package uk.gov.hmcts.reform.bulkscanprocessor.model.mapper;

import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.NonScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Payment;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.EnvelopeResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.NonScannableItemResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.PaymentResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.ScannableItemResponse;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

public final class EnvelopeResponseMapper {

    private EnvelopeResponseMapper() {
    }

    public static List<EnvelopeResponse> toEnvelopesResponse(List<Envelope> envelopes) {
        if (envelopes == null) {
            return emptyList();
        }
        return envelopes.stream()
            .map(EnvelopeResponseMapper::toEnvelopeResponse)
            .collect(Collectors.toList());
    }

    public static EnvelopeResponse toEnvelopeResponse(Envelope envelope) {
        if (envelope == null) {
            return null;
        }
        return new EnvelopeResponse(
            envelope.getId(),
            envelope.getCaseNumber(),
            envelope.getContainer(),
            envelope.getPoBox(),
            envelope.getJurisdiction(),
            envelope.getDeliveryDate(),
            envelope.getOpeningDate(),
            envelope.getZipFileCreateddate(),
            envelope.getZipFileName(),
            envelope.getStatus(),
            envelope.getClassification().toString(),
            envelope.getUrgent(),
            toScannableItemsResponse(envelope.getScannableItems()),
            toPaymentsResponse(envelope.getPayments()),
            toNonScannableItemsResponse(envelope.getNonScannableItems())
        );
    }

    private static List<ScannableItemResponse> toScannableItemsResponse(List<ScannableItem> scannableItems) {
        if (scannableItems == null) {
            return emptyList();
        }
        return scannableItems.stream()
            .map(EnvelopeResponseMapper::toScannableItemResponse)
            .collect(Collectors.toList());
    }


    private static ScannableItemResponse toScannableItemResponse(ScannableItem scannableItem) {
        if (scannableItem == null) {
            return null;
        }
        ScannableItemResponse response = new ScannableItemResponse(
            scannableItem.getDocumentControlNumber(),
            scannableItem.getScanningDate(),
            scannableItem.getOcrAccuracy(),
            scannableItem.getManualIntervention(),
            scannableItem.getNextAction(),
            scannableItem.getNextActionDate(),
            scannableItem.getOcrData(),
            scannableItem.getFileName(),
            scannableItem.getNotes(),
            scannableItem.getDocumentType()
        );
        response.setDocumentUrl(scannableItem.getDocumentUrl());
        return response;
    }

    private static List<NonScannableItemResponse> toNonScannableItemsResponse(
        List<NonScannableItem> nonScannableItems
    ) {
        if (nonScannableItems == null) {
            return emptyList();
        }
        return nonScannableItems.stream()
            .map(EnvelopeResponseMapper::toNonScannableItemResponse)
            .collect(Collectors.toList());
    }

    private static NonScannableItemResponse toNonScannableItemResponse(NonScannableItem nonScannableItem) {
        if (nonScannableItem == null) {
            return null;
        }
        return new NonScannableItemResponse(
            nonScannableItem.getDocumentControlNumber(),
            nonScannableItem.getItemType(),
            nonScannableItem.getNotes()
        );
    }

    private static List<PaymentResponse> toPaymentsResponse(List<Payment> payments) {
        if (payments == null) {
            return emptyList();
        }
        return payments.stream()
            .map(EnvelopeResponseMapper::toPaymentResponse)
            .collect(Collectors.toList());
    }

    private static PaymentResponse toPaymentResponse(Payment payment) {
        if (payment == null) {
            return null;
        }
        return new PaymentResponse(
            payment.getDocumentControlNumber(),
            payment.getMethod(),
            Double.toString(payment.getAmount()),
            payment.getCurrency(),
            payment.getPaymentInstrumentNumber(),
            payment.getSortCode(),
            payment.getAccountNumber()
        );
    }

}
