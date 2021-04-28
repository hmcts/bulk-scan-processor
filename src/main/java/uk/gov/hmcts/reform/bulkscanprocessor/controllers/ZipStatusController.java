package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.bulkscanprocessor.services.zipfilestatus.ZipFileStatusService;

import java.util.Map;

@RestController
@RequestMapping(
    path = "/zip-files",
    produces = MediaType.APPLICATION_JSON_VALUE
)
public class ZipStatusController {

    private final ZipFileStatusService service;
    private static final int MIN_DCN_LENGTH = 6;

    // region constructor

    public ZipStatusController(ZipFileStatusService service) {
        this.service = service;
    }

    // endregion

    @GetMapping
    public ResponseEntity getStatusByFilter(@RequestParam Map<String,String> filtersList) {

        if (filtersList.size() == 1) {

            if (filtersList.keySet().contains("name") && !filtersList.get("name").isEmpty()) {
                return ResponseEntity.ok(service.getStatusByFileName(filtersList.get("name")));
            }

            if (filtersList.keySet().contains("dcn") && !filtersList.get("dcn").isEmpty()) {
                var dcnLength = filtersList.get("dcn").length();
                if (dcnLength < MIN_DCN_LENGTH) {
                    return ResponseEntity.badRequest().body(
                        "Invalid dcn parameter. The minimum expected length of dcn is 6 characters."
                    );
                }
                return ResponseEntity.ok(service.getStatusByDcn(filtersList.get("dcn")));
            }

            if (filtersList.keySet().contains("ccd_id") && !filtersList.get("ccd_id").isEmpty()) {
                return ResponseEntity.ok(service.getStatusByCcdId(filtersList.get("ccd_id")));
            }
        }

        return ResponseEntity.badRequest().body("No records");
    }
}
