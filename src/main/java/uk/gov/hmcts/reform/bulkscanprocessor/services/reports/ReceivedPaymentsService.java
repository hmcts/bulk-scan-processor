package uk.gov.hmcts.reform.bulkscanprocessor.services.reports;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.ReceivedPayment;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.ReceivedPaymentRepository;

import java.time.LocalDate;
import java.util.List;

@Service
public class ReceivedPaymentsService {

    private final ReceivedPaymentRepository receivedPaymentRepository;

    public ReceivedPaymentsService(ReceivedPaymentRepository receivedPaymentRepository) {
        this.receivedPaymentRepository = receivedPaymentRepository;
    }

    public List<ReceivedPayment> getReceivedPayments(LocalDate date) {
        return receivedPaymentRepository.getReceivedPaymentsFor(date);
    }
}
