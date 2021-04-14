package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.zipfilestatus.ZipFileStatus;
import uk.gov.hmcts.reform.bulkscanprocessor.services.zipfilestatus.ZipFileStatusService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    @RequestMapping
    public ResponseEntity<List<ZipFileStatus>> findByFileNameOrDcn(@RequestParam Map<String,String> params) {
        String fileName = params.get("name");
        String dcn = params.get("dcn");
        if (fileName != null && dcn == null) {
            List<ZipFileStatus> zipFileStatuses = new ArrayList<>();
            zipFileStatuses.add(service.getStatusFor(fileName));
            return ResponseEntity.ok().body(zipFileStatuses);
        } else if (dcn != null && fileName == null) {
            return ResponseEntity.ok().body(service.getStatusByDcn(dcn));
        }
        return ResponseEntity.badRequest().body(null);
    }
}
