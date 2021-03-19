package uk.gov.hmcts.reform.bulkscanprocessor.services.payment;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.PaymentRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.model.in.PaymentInfo;

@Component
public class PaymentService {
    private final PaymentRepository repository;

    public PaymentService(PaymentRepository repository) {
        this.repository = repository;
    }

    public void updatePaymentStatus(PaymentInfo paymentInfo) {


    }
}
