package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import io.swagger.annotations.ApiOperation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.bulkscanprocessor.services.EnvelopeActionService;

import java.util.UUID;

@RestController
@RequestMapping(path = "/actions")
public class ActionController {
    private final EnvelopeActionService envelopeActionService;

    public ActionController(EnvelopeActionService envelopeActionService) {
        this.envelopeActionService = envelopeActionService;
    }

    @PutMapping(path = "/reprocess/{id}")
    @ApiOperation("Reprocess envelope by ID")
    public ResponseEntity<Void> reprocess(@PathVariable UUID id) {
        envelopeActionService.reprocessEnvelope(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PutMapping(path = "/make-completed/{id}")
    @ApiOperation("Reprocess envelope by ID")
    public ResponseEntity<Void> makeCompleted(@PathVariable UUID id) {
        envelopeActionService.moveEnvelopeToCompleted(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
