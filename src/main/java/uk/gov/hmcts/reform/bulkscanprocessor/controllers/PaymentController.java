package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.bulkscanprocessor.model.in.PaymentRequest;
import uk.gov.hmcts.reform.bulkscanprocessor.services.AuthService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.payment.PaymentService;

@RestController
@RequestMapping(path = "/payment")
public class PaymentController {
    private final AuthService authService;
    private final PaymentService paymentService;
    public static final String SUCCESSFUL_UPATE = "Payment status successfully updated";

    public PaymentController(AuthService authService, PaymentService paymentService) {
        this.authService = authService;
        this.paymentService = paymentService;
    }

    @PutMapping(path = "/status")
    @ApiOperation(value = "Update's payment status to REQUESTED")
    @ApiResponses({
        @ApiResponse(code = 200, message = SUCCESSFUL_UPATE),
        @ApiResponse(code = 401, message = "Invalid service authorisation header"),
        @ApiResponse(code = 403, message = "Service not configured")
    })
    public ResponseEntity<String> updatePayemnts(
        @RequestHeader(name = "ServiceAuthorization", required = false) String serviceAuthHeader,
        @RequestBody PaymentRequest paymentRequest) {

        authService.authenticate(serviceAuthHeader);
        paymentService.updatePaymentStatus(paymentRequest);
        return ResponseEntity.ok(SUCCESSFUL_UPATE);
    }
}

