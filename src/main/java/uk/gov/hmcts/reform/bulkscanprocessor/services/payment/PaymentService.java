package uk.gov.hmcts.reform.bulkscanprocessor.services.payment;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Payment;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.PaymentRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.PaymentRecordsException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.in.PaymentInfo;
import uk.gov.hmcts.reform.bulkscanprocessor.model.in.PaymentRequest;

import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

/**
 * Service to handle payment records.
 */
@Service
public class PaymentService {
    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository repository;

    /**
     * Constructor for PaymentService.
     * @param repository The payment repository
     */
    public PaymentService(PaymentRepository repository) {
        this.repository = repository;
    }

    /**
     * Updates the payment status.
     * @param paymentRequest The payment request
     */
    @Transactional
    public void updatePaymentStatus(PaymentRequest paymentRequest) {
        List<PaymentInfo> payments = paymentRequest.getPayments();

        if (payments != null && !payments.isEmpty()) {
            List<String> dcns = payments.stream()
                .map(PaymentInfo::getDocumentControlNumber)
                .collect(toList());

            logger.info("DCNS to be updated {}", dcns);

            int count = repository.updateStatus(dcns);
            logger.info("Records count updated {}", count);
            if (count != dcns.size()) {
                throw new PaymentRecordsException("Number of records updated don't match");
            }
        } else {
            throw new PaymentRecordsException("No payment DCN's to be update");
        }
    }

    /**
     * Get the payment records by DCN.
     * @param paymentDcns The list of payment DCN's
     */
    public Optional<List<Payment>> getPayment(List<String> paymentDcns) {
        return repository.findByDocumentControlNumberIn(paymentDcns);
    }

}

