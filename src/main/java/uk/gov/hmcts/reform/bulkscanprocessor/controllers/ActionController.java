package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import io.swagger.v3.oas.annotations.Operation;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.InvalidApiKeyException;
import uk.gov.hmcts.reform.bulkscanprocessor.services.EnvelopeActionService;

import java.util.UUID;

import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

/**
 * Controller for envelope actions.
 */
@RestController
@RequestMapping(path = "/actions")
public class ActionController {
    private static final Logger logger = getLogger(ActionController.class);

    private final EnvelopeActionService envelopeActionService;
    private final String actionsApiKey;

    /**
     * Constructor.
     * @param envelopeActionService service for envelope actions
     * @param apiKey API key for actions
     */
    public ActionController(
        EnvelopeActionService envelopeActionService,
        @Value("${actions.api-key}") String apiKey
    ) {
        this.envelopeActionService = envelopeActionService;
        this.actionsApiKey = apiKey;
    }

    /**
     * Reprocess envelope by ID.
     * @param authHeader API key
     * @param id envelope ID
     * @return 200 if successful
     */
    @PutMapping(path = "/reprocess/{id}")
    @Operation(description = "Reprocess envelope by ID")
    public ResponseEntity<Void> reprocess(
        @RequestHeader(value = AUTHORIZATION, required = false) String authHeader,
        @PathVariable UUID id
    ) {
        validateAuthorization(authHeader);

        envelopeActionService.reprocessEnvelope(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Update classification to EXCEPTION and reprocess envelope by ID.
     * @param authHeader API key
     * @param id envelope ID
     * @return 200 if successful
     */
    @PutMapping(path = "/update-classification-reprocess/{id}")
    @Operation(description = "Update classification to EXCEPTION and reprocess envelope by ID")
    public ResponseEntity<Void> updateClassificationAndReprocess(
        @RequestHeader(value = AUTHORIZATION, required = false) String authHeader,
        @PathVariable UUID id
    ) {
        validateAuthorization(authHeader);

        envelopeActionService.updateClassificationAndReprocessEnvelope(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Complete envelope by ID.
     * @param authHeader API key
     * @param id envelope ID
     * @return 200 if successful
     */
    @PutMapping(path = "/{id}/complete")
    @Operation(description = "Complete envelope by ID")
    public ResponseEntity<Void> makeCompleted(
        @RequestHeader(value = AUTHORIZATION, required = false) String authHeader,
        @PathVariable UUID id
    ) {
        validateAuthorization(authHeader);

        envelopeActionService.moveEnvelopeToCompleted(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Abort envelope by ID.
     * @param authHeader API key
     * @param id envelope ID
     * @return 200 if successful
     */
    @PutMapping(path = "/{id}/abort")
    @Operation(description = "Abort envelope by ID")
    public ResponseEntity<Void> makeAborted(
        @RequestHeader(value = AUTHORIZATION, required = false) String authHeader,
        @PathVariable UUID id
    ) {
        validateAuthorization(authHeader);

        envelopeActionService.moveEnvelopeToAborted(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Validate API key.
     * @param authorizationKey API key
     * @throws InvalidApiKeyException if API key is missing or invalid
     */
    private void validateAuthorization(String authorizationKey) {

        if (StringUtils.isEmpty(authorizationKey)) {
            logger.error("API Key is missing");
            throw new InvalidApiKeyException("API Key is missing");
        } else if (!authorizationKey.equals("Bearer " + actionsApiKey)) {
            logger.error("Invalid API Key");
            throw new InvalidApiKeyException("Invalid API Key");
        }

    }
}
