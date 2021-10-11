package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.bulkscanprocessor.model.in.PaymentRequest;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.PaymentResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.PaymentStatusReponse;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.SearchResult;
import uk.gov.hmcts.reform.bulkscanprocessor.services.AuthService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.payment.PaymentService;

import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;

import static uk.gov.hmcts.reform.bulkscanprocessor.model.mapper.EnvelopeResponseMapper.toPaymentResponse;

@Validated
@RestController
@RequestMapping(path = "/payment")
public class PaymentController {
    private final AuthService authService;
    private final PaymentService paymentService;
    public static final String SUCCESSFUL_UPDATE = "success";

    public PaymentController(AuthService authService, PaymentService paymentService) {
        this.authService = authService;
        this.paymentService = paymentService;
    }

    @PutMapping(path = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Update's payment status to SUBMITTED")
    @ApiResponses({
        @ApiResponse(code = 200, message = SUCCESSFUL_UPDATE),
        @ApiResponse(code = 401, message = "Invalid service authorisation header"),
        @ApiResponse(code = 403, message = "Service not configured"),
        @ApiResponse(code = 400, message = "Bad request")
    })
    public ResponseEntity<PaymentStatusReponse> updatePayments(
        @RequestHeader(name = "ServiceAuthorization", required = false) String serviceAuthHeader,
        @Valid @RequestBody PaymentRequest paymentRequest) {

        authService.authenticate(serviceAuthHeader);
        paymentService.updatePaymentStatus(paymentRequest);
        return ResponseEntity.ok().body(new PaymentStatusReponse(SUCCESSFUL_UPDATE));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public SearchResult getPaymentDcns(
        @RequestParam(name = "dcns") List<String> dcns) {
        final List<PaymentResponse> data = new ArrayList<>();
        paymentService
            .getPayment(dcns)
            .ifPresentOrElse(
                payments -> payments.stream()
                    .map(p -> toPaymentResponse(p))
                    .forEach(p -> data.add(p)),
                () -> new ArrayList<>()
            );

        return new SearchResult(data);
    }
}

