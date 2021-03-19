package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.bulkscanprocessor.model.in.PaymentInfo;
import uk.gov.hmcts.reform.bulkscanprocessor.services.AuthService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.payment.PaymentService;

@RestController
@RequestMapping(path = "/payment")
public class PaymentController {
    private final AuthService authService;
    private final PaymentService paymentService;

    public PaymentController(AuthService authService, PaymentService paymentService) {
        this.authService = authService;
        this.paymentService = paymentService;
    }

    @PutMapping(path = "/status")
    public void updatePayemnts(@RequestHeader(name = "ServiceAuthorization", required = false) String serviceAuthHeader,
                               @RequestBody PaymentInfo paymentInfo) {
        authService.authenticate(serviceAuthHeader);
        paymentService.updatePaymentStatus(paymentInfo);
    }
}
