package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.bulkscanprocessor.services.zipfilestatus.ZipFileStatusService;

import java.util.Map;

/**
 * Controller for zip file status actions.
 */
@RestController
@RequestMapping(
    path = "/zip-files",
    produces = MediaType.APPLICATION_JSON_VALUE
)
public class ZipStatusController {

    private final ZipFileStatusService service;
    private static final int MIN_DCN_LENGTH = 6;
    private static final String CCD_ID = "ccd_id";
    private static final String DCN = "dcn";
    private static final String FILE_NAME = "name";

    /**
     * Constructor for the zip file status controller.
     * @param service The service for zip file status
     */
    public ZipStatusController(ZipFileStatusService service) {
        this.service = service;
    }

    /**
     * Get the status of a zip file by filter.
     * @param filtersList The filters
     * @return The status of the zip file
     */
    @GetMapping
    public ResponseEntity getStatusByFilter(@RequestParam Map<String,String> filtersList) {

        if (filtersList.size() == 1) {

            if (filtersList.keySet().contains(FILE_NAME) && !filtersList.get(FILE_NAME).isEmpty()) {
                return ResponseEntity.ok(service.getStatusByFileName(filtersList.get(FILE_NAME)));
            }

            if (filtersList.keySet().contains(DCN) && !filtersList.get(DCN).isEmpty()) {
                var dcnLength = filtersList.get(DCN).length();
                if (dcnLength < MIN_DCN_LENGTH) {
                    return ResponseEntity.badRequest().body(
                        "Invalid dcn parameter. The minimum expected length of dcn is 6 characters."
                    );
                }
                return ResponseEntity.ok(service.getStatusByDcn(filtersList.get(DCN)));
            }

            if (filtersList.keySet().contains(CCD_ID) && !filtersList.get(CCD_ID).isEmpty()) {
                return ResponseEntity.ok(service.getStatusByCcdId(filtersList.get(CCD_ID)));
            }
        }

        return ResponseEntity.badRequest().body("No records");
    }
}
