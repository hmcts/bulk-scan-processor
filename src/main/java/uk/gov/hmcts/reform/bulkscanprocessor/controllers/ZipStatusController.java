package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.zipfilestatus.ZipFileStatus;
import uk.gov.hmcts.reform.bulkscanprocessor.services.zipfilestatus.ZipFileStatusService;

import java.security.InvalidParameterException;
import java.util.List;

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

    @GetMapping(params = {"name"})
    public ResponseEntity<ZipFileStatus> findByFileName(
        @RequestParam("name") String fileName,
        @RequestParam("dcn") String dcn
    ) {
        if (!fileName.equals(null) && dcn.equals(null)) {
            return ResponseEntity.ok().body(service.getStatusFor(fileName));
        } else {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping(params = {"dcn"})
    public ResponseEntity<List<ZipFileStatus>> findByDcn(
        @RequestParam("name") String fileName,
        @RequestParam("dcn") String dcn
    ) {
        if (dcn.equals(null) || !fileName.equals(null)) {
            return ResponseEntity.badRequest().body(null);
        }

        try {
            var result = service.getStatusByDcn(dcn);
            return ResponseEntity.ok().body(result);
        } catch (InvalidParameterException ex) {
            return ResponseEntity.badRequest().body(null);
        }
    }
}
