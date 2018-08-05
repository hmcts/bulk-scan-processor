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

public class EnvelopeResponseMapper {

    public EnvelopeResponse toEnvelopeResponse(Envelope envelope) {
        if (envelope == null) {
            return null;
        }
        EnvelopeResponse response = new EnvelopeResponse(
            envelope.getPoBox(),
            envelope.getJurisdiction(),
            envelope.getDeliveryDate(),
            envelope.getOpeningDate(),
            envelope.getZipFileCreateddate(),
            envelope.getZipFileName(),
            toScannableItemsResponse(envelope.getScannableItems()),
            toPaymentsResponse(envelope.getPayments()),
            toNonScannableItemsResponse(envelope.getNonScannableItems())
        );
        response.setContainer(envelope.getContainer());
        return response;
    }

    private List<ScannableItemResponse> toScannableItemsResponse(List<ScannableItem> scannableItems) {
        if (scannableItems == null) {
            return null;
        }
        return scannableItems.stream()
            .map(this::toScannableItemResponse)
            .collect(Collectors.toList());
    }

    private ScannableItemResponse toScannableItemResponse(ScannableItem scannableItem) {
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
            scannableItem.getNotes()
        );
        response.setDocumentUrl(scannableItem.getDocumentUrl());
        return response;
    }

    private List<NonScannableItemResponse> toNonScannableItemsResponse(List<NonScannableItem> nonScannableItems) {
        if (nonScannableItems == null) {
            return null;
        }
        return nonScannableItems.stream()
            .map(this::toNonScannableItemResponse)
            .collect(Collectors.toList());
    }

    private NonScannableItemResponse toNonScannableItemResponse(NonScannableItem nonScannableItem) {
        if (nonScannableItem == null) {
            return null;
        }
        return new NonScannableItemResponse(
            nonScannableItem.getItemType(),
            nonScannableItem.getNotes()
        );
    }

    private List<PaymentResponse> toPaymentsResponse(List<Payment> payments) {
        if (payments == null) {
            return null;
        }
        return payments.stream()
            .map(this::toPaymentResponse)
            .collect(Collectors.toList());
    }

    private PaymentResponse toPaymentResponse(Payment payment) {
        if (payment == null) {
            return null;
        }
        return new PaymentResponse(
            payment.getDocumentControlNumber(),
            payment.getMethod(),
            Double.toString(payment.getAmount()),
            payment.getCurrency()
        );
    }

}
