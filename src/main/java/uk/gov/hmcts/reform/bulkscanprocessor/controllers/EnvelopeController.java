package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.bulkscanprocessor.model.in.StatusUpdate;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.EnvelopeMetadataResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.services.AuthService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.EnvelopeRetrieverService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.EnvelopeUpdateService;

import java.util.UUID;

import static org.springframework.http.ResponseEntity.noContent;

@RestController
@RequestMapping(path = "envelopes", produces = MediaType.APPLICATION_JSON_VALUE)
public class EnvelopeController {

    private final EnvelopeRetrieverService envelopeRetrieverService;
    private final EnvelopeUpdateService envelopeUpdateService;
    private final AuthService authService;

    public EnvelopeController(
        EnvelopeRetrieverService envelopeRetrieverService,
        EnvelopeUpdateService envelopeUpdateService,
        AuthService authService
    ) {
        this.envelopeRetrieverService = envelopeRetrieverService;
        this.envelopeUpdateService = envelopeUpdateService;
        this.authService = authService;
    }

    @GetMapping
    @ApiOperation(
        value = "Retrieves all envelopes with processed status for a given jurisdiction",
        notes = "Returns an empty list when no envelopes were found"
    )
    @ApiResponse(code = 200, message = "Success", response = EnvelopeMetadataResponse.class)
    public EnvelopeMetadataResponse getProcessedEnvelopesByJurisdiction(
        @RequestHeader(name = "ServiceAuthorization", required = false) String serviceAuthHeader
    ) {
        String serviceName = authService.authenticate(serviceAuthHeader);

        return new EnvelopeMetadataResponse(
            envelopeRetrieverService.getProcessedEnvelopesByJurisdiction(serviceName)
        );
    }

    @PutMapping(path = "/{id}/status")
    @ApiOperation("Updates the status of given envelope")
    @ApiResponse(code = 204, message = "Envelope updated")
    public ResponseEntity<Void> updateStatus(
        @PathVariable UUID id,
        @RequestBody StatusUpdate statusUpdate,
        @RequestHeader(name = "ServiceAuthorization", required = false) String serviceAuthHeader
    ) {
        String serviceName = authService.authenticate(serviceAuthHeader);
        envelopeUpdateService.updateStatus(id, statusUpdate.status, serviceName);

        return noContent().build();
    }
}
