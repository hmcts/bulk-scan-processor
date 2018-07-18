package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.EnvelopeMetadataResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.services.AuthService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.EnvelopeRetrieverService;

@RestController
@RequestMapping(path = "envelopes", produces = MediaType.APPLICATION_JSON_VALUE)
public class EnvelopeController {

    private final EnvelopeRetrieverService envelopeRetrieverService;
    private final AuthService authService;

    public EnvelopeController(
        EnvelopeRetrieverService envelopeRetrieverService,
        AuthService authService
    ) {
        this.envelopeRetrieverService = envelopeRetrieverService;
        this.authService = authService;
    }

    @GetMapping
    @ApiOperation(
        value = "Retrieves all envelopes for a given jurisdiction",
        notes = "Returns an empty list when no envelopes were found"
    )
    @ApiResponse(code = 200, message = "Success", response = EnvelopeMetadataResponse.class)
    public EnvelopeMetadataResponse getEnvelopesByJurisdiction(
        @RequestHeader(name = "ServiceAuthorization", required = false) String serviceAuthHeader
    ) {
        String serviceName = authService.authenticate(serviceAuthHeader);

        return new EnvelopeMetadataResponse(
            envelopeRetrieverService.getAllEnvelopesForJurisdiction(serviceName)
        );
    }
}
