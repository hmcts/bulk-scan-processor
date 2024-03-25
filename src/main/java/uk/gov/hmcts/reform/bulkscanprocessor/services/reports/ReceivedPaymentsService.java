package uk.gov.hmcts.reform.bulkscanprocessor.services.reports;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.ReceivedPayment;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.ReceivedPaymentRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * Service to handle received payments.
 */
@Service
public class ReceivedPaymentsService {

    private final ReceivedPaymentRepository receivedPaymentRepository;

    /**
     * Constructor for the ReceivedPaymentsService.
     * @param receivedPaymentRepository The repository for received payment
     */
    public ReceivedPaymentsService(ReceivedPaymentRepository receivedPaymentRepository) {
        this.receivedPaymentRepository = receivedPaymentRepository;
    }

    /**
     * Get the received payments for a date.
     * @param date The date
     * @return The received payments
     */
    public List<ReceivedPayment> getReceivedPayments(LocalDate date) {
        return receivedPaymentRepository.getReceivedPaymentsFor(date);
    }
}
