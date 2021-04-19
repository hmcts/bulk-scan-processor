package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.zipfilestatus.ZipFileStatus;
import uk.gov.hmcts.reform.bulkscanprocessor.services.zipfilestatus.ZipFileStatusService;

import java.util.Optional;

@RestController
@RequestMapping(
    path = "/zip-files",
    produces = MediaType.APPLICATION_JSON_VALUE
)
public class ZipStatusController {

    private final ZipFileStatusService service;

    // region constructor
    public ZipStatusController(ZipFileStatusService service) {
        this.service = service;
    }
    // endregion

    @GetMapping
    public ResponseEntity<?> getStatus(@RequestParam(value = "name", required = false) Optional<String> fileName, @RequestParam(value = "ccd_id", required = false) Optional<String> ccdId) {

        if (fileName.isPresent() && !ccdId.isPresent()){
           return ResponseEntity.ok(service.getStatusFor(fileName.get()));
        }
        else if (!fileName.isPresent() && ccdId.isPresent()){
            return ResponseEntity.ok(service.getStatusByCcdId(ccdId.get()));
        }
        return ResponseEntity.badRequest().body(null);
    }
}
