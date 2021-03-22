package uk.gov.hmcts.reform.bulkscanprocessor.services.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.PaymentRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.model.in.PaymentInfo;
import uk.gov.hmcts.reform.bulkscanprocessor.model.in.PaymentRequest;

import java.time.LocalDateTime;
import java.util.List;
import javax.transaction.Transactional;

import static java.util.stream.Collectors.toList;

@Service
public class PaymentService {
    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository repository;

    public PaymentService(PaymentRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void updatePaymentStatus(PaymentRequest paymentRequest) {
        List<PaymentInfo> payments = paymentRequest.payments;

        if (payments != null && !payments.isEmpty()) {
            List<String> dcns = payments.stream().map(paymentInfo -> paymentInfo.documentControlNumber)
                .collect(toList());
            logger.info("DCNS to be updated {}", dcns);

            int count = repository.updateStatus(LocalDateTime.now(), dcns);
            logger.info("Records count updated {}", count);
        } else {
            logger.error("Nothing to update");
        }
    }
}
