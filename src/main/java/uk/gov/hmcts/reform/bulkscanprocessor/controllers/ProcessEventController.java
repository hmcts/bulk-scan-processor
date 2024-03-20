package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.SearchResult;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.ProcessEventsService;

import java.time.LocalDate;
import java.util.List;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

/**
 * Controller for process events.
 */
@Validated
@RestController
@RequestMapping(path = "/process-events")
public class ProcessEventController {
    private final ProcessEventsService processEventsService;

    /**
     * Constructor for the process event controller.
     * @param processEventsService The service for process events
     */
    public ProcessEventController(ProcessEventsService processEventsService) {
        this.processEventsService = processEventsService;
    }

    /**
     * Get process events by dcn prefix and date.
     * @param dcnPrefix The dcn prefix
     * @param dates The dates
     * @return Process events by dcn prefix and date
     */
    @GetMapping(params = {"dcn_prefix", "between_dates"})
    public ResponseEntity findProcessEventsByDcnPrefixAndDate(
            @RequestParam(name = "dcn_prefix") String dcnPrefix,
            @RequestParam(name = "between_dates") @DateTimeFormat(iso = DATE) List<LocalDate> dates
    ) {

        if (dcnPrefix.length() < 10) {
            return ResponseEntity.badRequest().body("`dcn_prefix` should contain at least 10 characters");
        }
        if (dates.size() != 2) {
            return ResponseEntity.badRequest().body("`between_dates` should contain 2 valid dates");
        }
        LocalDate fromDate = dates.get(0);
        LocalDate toDate = dates.get(1);
        if (toDate.isBefore(fromDate)) {
            toDate = dates.get(0);
            fromDate = dates.get(1);
        }

        return ResponseEntity.ok(new SearchResult(
                processEventsService.getProcessEventsByDcnPrefix(dcnPrefix, fromDate, toDate)
            )
        );
    }
}
