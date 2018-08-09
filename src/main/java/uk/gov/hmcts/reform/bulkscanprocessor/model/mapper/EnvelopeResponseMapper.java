package uk.gov.hmcts.reform.bulkscanprocessor.model.mapper;

import com.google.common.collect.ImmutableList;
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

    public List<EnvelopeResponse> toEnvelopesResponse(List<Envelope> envelopes) {
        if (envelopes == null) {
            return ImmutableList.of();
        }
        return envelopes.stream()
            .map(this::toEnvelopeResponse)
            .collect(Collectors.toList());
    }

    public EnvelopeResponse toEnvelopeResponse(Envelope envelope) {
        if (envelope == null) {
            return null;
        }
        return new EnvelopeResponse(
            envelope.getId(),
            envelope.getContainer(),
            envelope.getPoBox(),
            envelope.getJurisdiction(),
            envelope.getDeliveryDate(),
            envelope.getOpeningDate(),
            envelope.getZipFileCreateddate(),
            envelope.getZipFileName(),
            envelope.getStatus(),
            toScannableItemsResponse(envelope.getScannableItems()),
            toPaymentsResponse(envelope.getPayments()),
            toNonScannableItemsResponse(envelope.getNonScannableItems())
        );
    }

    private List<ScannableItemResponse> toScannableItemsResponse(List<ScannableItem> scannableItems) {
        if (scannableItems == null) {
            return ImmutableList.of();
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
            return ImmutableList.of();
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
            return ImmutableList.of();
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
