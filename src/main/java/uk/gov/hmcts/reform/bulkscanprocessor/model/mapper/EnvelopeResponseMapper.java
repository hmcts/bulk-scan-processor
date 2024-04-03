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
import static org.springframework.util.CollectionUtils.isEmpty;

/**
 * Maps database envelope to response envelope.
 */
public final class EnvelopeResponseMapper {

    /**
     * Constructor.
     */
    private EnvelopeResponseMapper() {
    }

    /**
     * Maps database envelope to response envelope.
     *
     * @param envelopes list of envelopes
     * @return list of response envelopes
     */
    public static List<EnvelopeResponse> toEnvelopesResponse(List<Envelope> envelopes) {
        if (envelopes == null) {
            return emptyList();
        }
        return envelopes.stream()
            .map(EnvelopeResponseMapper::toEnvelopeResponse)
            .collect(Collectors.toList());
    }

    /**
     * Maps database envelope to response envelope.
     *
     * @param envelope envelope
     * @return response envelope
     */
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
            envelope.getRescanFor(),
            envelope.getStatus(),
            envelope.getClassification().toString(),
            toScannableItemsResponse(envelope.getScannableItems()),
            toPaymentsResponse(envelope.getPayments()),
            toNonScannableItemsResponse(envelope.getNonScannableItems()),
            envelope.getCcdId(),
            envelope.getEnvelopeCcdAction()
        );
    }

    /**
     * Maps database scannable items to response scannable items.
     *
     * @param scannableItems list of scannable items
     * @return list of response scannable items
     */
    public static List<ScannableItemResponse> toScannableItemsResponse(List<ScannableItem> scannableItems) {
        if (scannableItems == null) {
            return emptyList();
        }
        return scannableItems.stream()
            .map(EnvelopeResponseMapper::toScannableItemResponse)
            .collect(Collectors.toList());
    }

    /**
     * Maps database scannable item to response scannable item.
     *
     * @param scannableItem scannable item
     * @return response scannable item
     */
    private static ScannableItemResponse toScannableItemResponse(ScannableItem scannableItem) {
        if (scannableItem == null) {
            return null;
        }

        return new ScannableItemResponse(
            scannableItem.getDocumentControlNumber(),
            scannableItem.getScanningDate(),
            scannableItem.getOcrAccuracy(),
            scannableItem.getManualIntervention(),
            scannableItem.getNextAction(),
            scannableItem.getNextActionDate(),
            scannableItem.getFileName(),
            scannableItem.getDocumentUuid(),
            scannableItem.getDocumentType(),
            scannableItem.getDocumentSubtype(),
            scannableItem.getOcrData() != null && !isEmpty(scannableItem.getOcrData().fields)
        );
    }

    /**
     * Maps database non-scannable items to response non-scannable items.
     *
     * @param nonScannableItems list of non-scannable items
     * @return list of response non-scannable items
     */
    public static List<NonScannableItemResponse> toNonScannableItemsResponse(
        List<NonScannableItem> nonScannableItems
    ) {
        if (nonScannableItems == null) {
            return emptyList();
        }
        return nonScannableItems.stream()
            .map(EnvelopeResponseMapper::toNonScannableItemResponse)
            .collect(Collectors.toList());
    }

    /**
     * Maps database non-scannable item to response non-scannable item.
     *
     * @param nonScannableItem non-scannable item
     * @return response non-scannable item
     */
    private static NonScannableItemResponse toNonScannableItemResponse(NonScannableItem nonScannableItem) {
        if (nonScannableItem == null) {
            return null;
        }
        return new NonScannableItemResponse(
            nonScannableItem.getDocumentControlNumber(),
            nonScannableItem.getItemType()
        );
    }

    /**
     * Maps database payments to response payments.
     *
     * @param payments list of payments
     * @return list of response payments
     */
    public static List<PaymentResponse> toPaymentsResponse(List<Payment> payments) {
        if (payments == null) {
            return emptyList();
        }
        return payments.stream()
            .map(EnvelopeResponseMapper::toPaymentResponse)
            .collect(Collectors.toList());
    }

    /**
     * Maps database payment to response payment.
     *
     * @param payment payment
     * @return response payment
     */
    public static PaymentResponse toPaymentResponse(Payment payment) {
        if (payment == null) {
            return null;
        } else {
            return new PaymentResponse(
                payment.getId(),
                payment.getDocumentControlNumber(),
                payment.getStatus(),
                payment.getLastmodified()
            );
        }
    }

}
