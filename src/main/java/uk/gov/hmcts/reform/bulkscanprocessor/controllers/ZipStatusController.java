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
    private final int minDcnLength = 6;

    // region constructor
    public ZipStatusController(ZipFileStatusService service) {
        this.service = service;
    }
    // endregion

    @RequestMapping
    public ResponseEntity<List<ZipFileStatus>> findFileByFilter(@RequestParam Map<String,String> filtersList) {

        //invalid parameter list
        if (filtersList.isEmpty()
            || filtersList.keySet().size() > 1
            || ((filtersList.keySet().size() == 1) && filtersList.values().toArray()[0].equals(""))) {
            return ResponseEntity.badRequest().body(null);
        }

        if (filtersList.keySet().contains("name")) {
            List<ZipFileStatus> zipFileStatuses = new ArrayList<>();
            zipFileStatuses.add(service.getStatusFor(filtersList.get("name")));
            return ResponseEntity.ok().body(zipFileStatuses);
        }

        if (filtersList.keySet().contains("dcn")) {
            var dcnLength = filtersList.get("dcn").length();
            if (dcnLength < minDcnLength) {
                return ResponseEntity.badRequest().body(null);
            }
            return ResponseEntity.ok().body(service.getStatusByDcn(filtersList.get("dcn")));
        }

        return ResponseEntity.badRequest().body(null);
    }
}
