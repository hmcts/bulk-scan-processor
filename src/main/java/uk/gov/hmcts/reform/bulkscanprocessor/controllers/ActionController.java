package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import io.swagger.annotations.ApiOperation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.bulkscanprocessor.services.EnvelopeReprocessService;

import java.util.UUID;

@RestController
@RequestMapping(path = "/actions")
public class ActionController {
    private final EnvelopeReprocessService envelopeReprocessService;

    public ActionController(EnvelopeReprocessService envelopeReprocessService) {
        this.envelopeReprocessService = envelopeReprocessService;
    }

    @PutMapping(path = "/reprocess/{id}")
    @ApiOperation("Read single envelope by ID")
    public ResponseEntity<Void> reprocess(@PathVariable UUID id) {
        envelopeReprocessService.reprocessEnvelope(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
