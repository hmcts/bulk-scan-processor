package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.Min;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EnvelopeNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.EnvelopeInfo;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.EnvelopeListResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.EnvelopeResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.SearchResult;
import uk.gov.hmcts.reform.bulkscanprocessor.services.AuthService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.EnvelopeRetrieverService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.IncompleteEnvelopesService;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for envelope actions.
 */
@Validated
@RestController
@RequestMapping(path = "envelopes", produces = MediaType.APPLICATION_JSON_VALUE)
public class EnvelopeController {

    private final EnvelopeRetrieverService envelopeRetrieverService;
    private final IncompleteEnvelopesService incompleteEnvelopesService;
    private final AuthService authService;

    private static final String DEFAULT_STALE_TIME_HOURS = "2";

    /**
     * Constructor for the envelope controller.
     * @param envelopeRetrieverService The service for retrieving envelopes
     * @param incompleteEnvelopesService The service for incomplete envelopes
     * @param authService The service for authentication
     */
    public EnvelopeController(
        EnvelopeRetrieverService envelopeRetrieverService,
        IncompleteEnvelopesService incompleteEnvelopesService,
        AuthService authService
    ) {
        this.envelopeRetrieverService = envelopeRetrieverService;
        this.incompleteEnvelopesService = incompleteEnvelopesService;
        this.authService = authService;
    }

    /**
     * Retrieves all envelopes.
     * @param serviceAuthHeader The service authorization header
     * @param status The status of the envelope
     * @return The list of envelopes
     */
    @GetMapping
    @Operation(
        summary = "Retrieves all envelopes",
        description = "Returns an empty list when no envelopes were found"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Success",
        content = @Content(schema = @Schema(implementation = EnvelopeListResponse.class))
    )
    public EnvelopeListResponse getAll(
        @RequestHeader(name = "ServiceAuthorization", required = false) String serviceAuthHeader,
        @RequestParam(name = "status", required = false) Status status
    ) {
        String serviceName = authService.authenticate(serviceAuthHeader);

        List<EnvelopeResponse> envelopes = envelopeRetrieverService.findByServiceAndStatus(serviceName, status);

        return new EnvelopeListResponse(envelopes);
    }

    /**
     * Retrieves a single envelope by ID.
     * @param serviceAuthHeader The service authorization header
     * @param id The ID of the envelope
     * @return The envelope
     */
    @GetMapping(path = "/{id}")
    @Operation(description = "Read single envelope by ID")
    public EnvelopeResponse getById(
        @RequestHeader(name = "ServiceAuthorization", required = false) String serviceAuthHeader,
        @PathVariable UUID id
    ) {
        String serviceName = authService.authenticate(serviceAuthHeader);
        return envelopeRetrieverService
            .findById(serviceName, id)
            .orElseThrow(EnvelopeNotFoundException::new);
    }

    /**
     * Retrieves a single envelope by filename and container.
     * @param container The container of the envelope
     * @param fileName The filename of the envelope
     * @return The envelope
     */
    @GetMapping(path = "/{container}/{file_name}")
    @Operation(description = "Read single envelope by filename and container", hidden = true)
    public EnvelopeResponse getByContainerAndFileName(
        @PathVariable(name = "container") String container,
        @PathVariable(name = "file_name") String fileName
    ) {
        return envelopeRetrieverService
            .findByFileNameAndContainer(fileName, container);
    }

    /**
     * Retrieves incomplete stale envelopes.
     * @param staleTime The time after which an envelope is considered stale
     * @return The list of incomplete stale envelopes
     */
    @GetMapping(path = "/stale-incomplete-envelopes")
    @Operation(
        summary = "Retrieves incomplete stale envelopes",
        description = "Returns an empty list when no incomplete stale envelopes were found"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Success",
        content = @Content(schema = @Schema(implementation = EnvelopeListResponse.class))
    )
    public SearchResult getIncomplete(
        @RequestParam(name = "stale_time", required = false, defaultValue = DEFAULT_STALE_TIME_HOURS)
            int staleTime
    ) {
        List<EnvelopeInfo> envelopes = incompleteEnvelopesService.getIncompleteEnvelopes(2);

        return new SearchResult(envelopes);
    }

    /**
     * Deletes a single stale envelope by ID.
     * @param staleTime The time after which an envelope is considered stale
     * @param envelopeId The ID of the envelope
     * @return The list of deleted envelopes
     */
    @DeleteMapping(path = "/stale/{envelopeId}")
    @Operation(
        summary = "Remove one stale envelope",
        description = "If an envelope is older than a certain period of time, remove it"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Success",
        content = @Content(schema = @Schema(implementation = EnvelopeListResponse.class))
    )
    @ApiResponse(responseCode = "404", description = "envelope not found")
    public SearchResult deleteOneStaleEnvelope(
        @RequestParam(name = "stale_time", required = false, defaultValue = "168")
        @Min(value = 168, message = "stale_time must be at least 168 hours (a week)")
            int staleTime,
        @PathVariable UUID envelopeId
    ) {
        if (incompleteEnvelopesService.deleteIncompleteEnvelopes(staleTime, List.of(envelopeId.toString())) == 0) {
            throw new EnvelopeNotFoundException("Envelope not removed, as it is not found/not stale");
        }
        return new SearchResult(List.of(envelopeId));
    }

    /**
     * Deletes all stale envelopes.
     * @param staleTime The time after which an envelope is considered stale
     * @return The list of deleted envelopes
     */
    @DeleteMapping(path = "stale/all")
    @Operation(
        summary = "Remove all stale envelopes",
        description = "If an envelope is older than a certain period of time, remove it"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Success",
        content = @Content(schema = @Schema(implementation = EnvelopeListResponse.class))
    )
    public SearchResult deleteAllStaleEnvelopes(
        @RequestParam(name = "stale_time", required = false, defaultValue = "168")
        @Min(value = 48, message = "stale_time must be at least 48 hours")
            int staleTime
    ) {
        List<EnvelopeInfo> envelopeInfo = incompleteEnvelopesService.getIncompleteEnvelopes(staleTime);
        List<String> envelopeIds = envelopeInfo.stream().map(s -> s.envelopeId.toString()).collect(Collectors.toList());
        incompleteEnvelopesService.deleteIncompleteEnvelopes(staleTime, envelopeIds);
        return new SearchResult(envelopeIds);
    }
}
