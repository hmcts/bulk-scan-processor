package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.zipfilestatus.ZipFileStatus;
import uk.gov.hmcts.reform.bulkscanprocessor.services.zipfilestatus.ZipFileStatusService;

import java.util.List;

@Validated
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

    @RequestMapping(params = "name")
    public ResponseEntity<ZipFileStatus> findByFileName(
        @RequestParam(required = false, value = "name") String fileName
    ) {
        if (fileName != "") {
            return ResponseEntity.ok().body(service.getStatusFor(fileName));
        }
        return ResponseEntity.badRequest().body(null);
    }

    @RequestMapping(params = "dcn")
    public ResponseEntity<List<ZipFileStatus>> findByDcn(
        @RequestParam(required = true, value = "dcn") String dcn
    ) {
        var result = service.getStatusByDcn(dcn);
        return ResponseEntity.ok().body(result);
    }
}
