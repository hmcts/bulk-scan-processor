package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.SasTokenResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.services.SasTokenGeneratorService;

@RestController
@RequestMapping(path = "/token")
public class SasTokenController {

    private final SasTokenGeneratorService tokenGeneratorService;

    public SasTokenController(SasTokenGeneratorService tokenGeneratorService) {
        this.tokenGeneratorService = tokenGeneratorService;
    }

    @GetMapping(path = "/{serviceName}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get SAS Token to access blob storage")
    @ApiResponse(code = 200, message = "Success")
    public ResponseEntity<SasTokenResponse> getSasToken(@PathVariable String serviceName) {
        String sasToken = tokenGeneratorService.generateSasToken(serviceName);

        return ResponseEntity.ok(new SasTokenResponse(sasToken));
    }
}
