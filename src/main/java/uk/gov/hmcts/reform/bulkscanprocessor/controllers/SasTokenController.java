package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.SasTokenResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.services.SasTokenGeneratorService;

/**
 * Controller for generating SAS tokens.
 */
@RestController
@RequestMapping(path = "/token")
public class SasTokenController {

    private final SasTokenGeneratorService tokenGeneratorService;

    /**
     * Constructor for the SAS token controller.
     * @param tokenGeneratorService The service for generating SAS tokens
     */
    public SasTokenController(SasTokenGeneratorService tokenGeneratorService) {
        this.tokenGeneratorService = tokenGeneratorService;
    }

    /**
     * Get SAS Token to access blob storage.
     * @param serviceName The name of the service
     * @return The SAS token response
     */
    @GetMapping(path = "/{serviceName}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(description = "Get SAS Token to access blob storage")
    @ApiResponse(responseCode = "200", description = "Success")
    public ResponseEntity<SasTokenResponse> getSasToken(@PathVariable String serviceName) {
        String sasToken = tokenGeneratorService.generateSasToken(serviceName);

        return ResponseEntity.ok(new SasTokenResponse(sasToken));
    }
}
