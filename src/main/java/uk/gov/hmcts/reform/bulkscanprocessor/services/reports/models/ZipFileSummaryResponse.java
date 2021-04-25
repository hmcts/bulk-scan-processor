package uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models;

import uk.gov.hmcts.reform.bulkscanprocessor.model.out.PaymentResponse;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class ZipFileSummaryResponse {

    public final String fileName;
    public final LocalDate dateReceived;
    public final LocalTime timeReceived;
    public final LocalDate dateProcessed;
    public final LocalTime timeProcessed;
    public final String container;
    public final String lastEventStatus;
    public final String envelopeStatus;
    public final String classification;
    public final String ccdId;
    public final String ccdAction;
    public final List<PaymentResponse> payments;

    // region constructor
    public ZipFileSummaryResponse(
        String fileName,
        LocalDate dateReceived,
        LocalTime timeReceived,
        LocalDate dateProcessed,
        LocalTime timeProcessed,
        String container,
        String lastEventStatus,
        String envelopeStatus,
        String classification,
        String ccdId,
        String ccdAction,
        List<PaymentResponse> payments
    ) {
        this.fileName = fileName;
        this.dateReceived = dateReceived;
        this.timeReceived = timeReceived;
        this.dateProcessed = dateProcessed;
        this.timeProcessed = timeProcessed;
        this.container = container;
        this.lastEventStatus = lastEventStatus;
        this.envelopeStatus = envelopeStatus;
        this.classification = classification;
        this.ccdId = ccdId;
        this.ccdAction = ccdAction;
        this.payments = payments;
    }
    // endregion
}
