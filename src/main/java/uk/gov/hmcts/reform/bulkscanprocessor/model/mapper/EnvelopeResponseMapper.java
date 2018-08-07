package uk.gov.hmcts.reform.bulkscanprocessor.model.mapper;

import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.NonScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Payment;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.EnvelopeResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.NonScannableItemResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.PaymentResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.ScannableItemResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.StatusResponse;

import java.util.List;
import java.util.stream.Collectors;

public class EnvelopeResponseMapper {

    public List<EnvelopeResponse> toEnvelopeResponses(List<Envelope> envelopes) {
        if (envelopes == null) {
            return null;
        }
        return envelopes.stream()
            .map(this::toEnvelopeResponse)
            .collect(Collectors.toList());
    }

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
        response.setId(envelope.getId());
        response.setStatus(envelope.getStatus());
        return response;
    }

    private StatusResponse toStatusResponse(Status status) {
        switch (status) {
            case CONSUMED:
                return StatusResponse.CONSUMED;
            case CREATED:
                return StatusResponse.CREATED;
            case PROCESSED:
                return StatusResponse.PROCESSED;
            case UPLOAD_FAILURE:
                return StatusResponse.UPLOAD_FAILURE;
            case UPLOADED:
                return StatusResponse.UPLOADED;
            default:
                return null;
        }
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



    public List<Envelope> toEnvelopes(List<EnvelopeResponse> envelopesResponse) {
        if (envelopesResponse == null) {
            return null;
        }
        return envelopesResponse.stream()
            .map(this::toEnvelope)
            .collect(Collectors.toList());
    }

    public Envelope toEnvelope(EnvelopeResponse envelopeResponse) {
        if (envelopeResponse == null) {
            return null;
        }
        Envelope envelope = new Envelope(
            envelopeResponse.getPoBox(),
            envelopeResponse.getJurisdiction(),
            envelopeResponse.getDeliveryDate(),
            envelopeResponse.getOpeningDate(),
            envelopeResponse.getZipFileCreateddate(),
            envelopeResponse.getZipFileName(),
            toScannableItems(envelopeResponse.getScannableItems()),
            toPayments(envelopeResponse.getPayments()),
            toNonScannableItems(envelopeResponse.getNonScannableItems())
        );
        return envelope;
    }

    private List<ScannableItem> toScannableItems(List<ScannableItemResponse> scannableItemsResponse) {
        if (scannableItemsResponse == null) {
            return null;
        }
        return scannableItemsResponse.stream()
            .map(this::toScannableItem)
            .collect(Collectors.toList());
    }

    private ScannableItem toScannableItem(ScannableItemResponse scannableItemResponse) {
        if (scannableItemResponse == null) {
            return null;
        }
        ScannableItem scannableItem = new ScannableItem(
            scannableItemResponse.getDocumentControlNumber(),
            scannableItemResponse.getScanningDate(),
            scannableItemResponse.getOcrAccuracy(),
            scannableItemResponse.getManualIntervention(),
            scannableItemResponse.getNextAction(),
            scannableItemResponse.getNextActionDate(),
            scannableItemResponse.getOcrData(),
            scannableItemResponse.getFileName(),
            scannableItemResponse.getNotes()
        );
        scannableItem.setDocumentUrl(scannableItemResponse.getDocumentUrl());
        return scannableItem;
    }

    private List<NonScannableItem> toNonScannableItems(List<NonScannableItemResponse> nonScannableItemsResponse) {
        if (nonScannableItemsResponse == null) {
            return null;
        }
        return nonScannableItemsResponse.stream()
            .map(this::toNonScannableItem)
            .collect(Collectors.toList());
    }

    private NonScannableItem toNonScannableItem(NonScannableItemResponse nonScannableItemResponse) {
        if (nonScannableItemResponse == null) {
            return null;
        }
        return new NonScannableItem(
            nonScannableItemResponse.getItemType(),
            nonScannableItemResponse.getNotes()
        );
    }

    private List<Payment> toPayments(List<PaymentResponse> paymentsResponse) {
        if (paymentsResponse == null) {
            return null;
        }
        return paymentsResponse.stream()
            .map(this::toPayment)
            .collect(Collectors.toList());
    }

    private Payment toPayment(PaymentResponse paymentResponse) {
        if (paymentResponse == null) {
            return null;
        }
        return new Payment(
            paymentResponse.getDocumentControlNumber(),
            paymentResponse.getMethod(),
            Double.toString(paymentResponse.getAmount()),
            paymentResponse.getCurrency()
        );
    }

}
