package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
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

    @GetMapping(params = {"name","ccd_id"})
    public RequestEntity<?> getStatus(@RequestParam("name") Optional<String> fileName, @RequestParam("ccd_id") Optional<String> ccdId) {
        if (fileName.isPresent() && !ccdId.isPresent()){
            return RequestEntityservice.getStatusFor(fileName);
        }

    }
    @GetMapping(params = {"name"})
    public ZipFileStatus findByFileName(@RequestParam("name") String fileName) {
        return service.getStatusFor(fileName);
    }

    @GetMapping(params = {"ccd_id"})
    public ZipFileStatus findByCcdId(@RequestParam("ccd_id") String ccdId) {
        return service.getStatusByCcdId(ccdId);
    }
}
