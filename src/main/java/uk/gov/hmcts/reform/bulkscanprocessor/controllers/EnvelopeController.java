package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EnvelopeNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.BlobInfo;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.EnvelopeListResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.EnvelopeResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.SearchResult;
import uk.gov.hmcts.reform.bulkscanprocessor.services.AuthService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.EnvelopeRetrieverService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.IncompleteEnvelopesService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(path = "envelopes", produces = MediaType.APPLICATION_JSON_VALUE)
public class EnvelopeController {

    private final EnvelopeRetrieverService envelopeRetrieverService;
    private final IncompleteEnvelopesService incompleteEnvelopesService;
    private final AuthService authService;

    private static final String DEFAULT_STALE_TIME_HOURS = "2";

    public EnvelopeController(
        EnvelopeRetrieverService envelopeRetrieverService,
        IncompleteEnvelopesService incompleteEnvelopesService,
        AuthService authService
    ) {
        this.envelopeRetrieverService = envelopeRetrieverService;
        this.incompleteEnvelopesService = incompleteEnvelopesService;
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

    @GetMapping(path = "/stale-incomplete-blobs")
    @ApiOperation(
        value = "Retrieves incomplete stale envelopes",
        notes = "Returns an empty list when no incomplete stale envelopes were found"
    )
    @ApiResponse(code = 200, message = "Success", response = EnvelopeListResponse.class)
    public SearchResult getIncomplete(
        @RequestParam(name = "stale_time", required = false, defaultValue = DEFAULT_STALE_TIME_HOURS)
            int staleTime
    ) {
        List<BlobInfo> envelopes = incompleteEnvelopesService.getIncompleteEnvelopes(2);

        return new SearchResult(envelopes);
    }
}
