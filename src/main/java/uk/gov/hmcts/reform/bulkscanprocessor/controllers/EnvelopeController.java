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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EnvelopeNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.model.in.StatusUpdate;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.EnvelopeListResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.EnvelopeResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.services.AuthService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.EnvelopeRetrieverService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.EnvelopeUpdateService;

import java.util.List;
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
        value = "Retrieves all envelopes",
        notes = "Returns an empty list when no envelopes were found"
    )
    @ApiResponse(code = 200, message = "Success", response = EnvelopeListResponse.class)
    public EnvelopeListResponse getAll(
        @RequestHeader(name = "ServiceAuthorization", required = false) String serviceAuthHeader,
        @RequestParam(name = "status", required = false) Status status
    ) {
        String serviceName = authService.authenticate(serviceAuthHeader);

        List<EnvelopeResponse> envelopes = envelopeRetrieverService.findByServiceAndStatus(serviceName, status);

        return new EnvelopeListResponse(envelopes);
    }

    @GetMapping(path = "/{id}")
    @ApiOperation("Read single envelope by ID")
    public EnvelopeResponse getById(
        @RequestHeader(name = "ServiceAuthorization", required = false) String serviceAuthHeader,
        @PathVariable UUID id
    ) {
        String serviceName = authService.authenticate(serviceAuthHeader);
        return envelopeRetrieverService
            .findById(serviceName, id)
            .orElseThrow(EnvelopeNotFoundException::new);
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
