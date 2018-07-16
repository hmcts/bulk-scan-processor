package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.EnvelopeMetadataResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.services.EnvelopeRetrieverService;

@RestController
@RequestMapping(path = "envelopes", produces = MediaType.APPLICATION_JSON_VALUE)
public class EnvelopeController {

    private final EnvelopeRetrieverService envelopeRetrieverService;

    public EnvelopeController(EnvelopeRetrieverService envelopeRetrieverService) {
        this.envelopeRetrieverService = envelopeRetrieverService;
    }

    @GetMapping
    @ApiOperation(value = "Retrieves all envelopes", notes = "Returns an empty list when no envelopes were found")
    @ApiResponse(code = 200, message = "Success", response = EnvelopeMetadataResponse.class)
    public EnvelopeMetadataResponse getEnvelopes() {
        return new EnvelopeMetadataResponse(
            envelopeRetrieverService.getAllEnvelopes()
        );
    }
}
