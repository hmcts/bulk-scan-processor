package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.reports.EnvelopeCountSummaryReportItem;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.reports.EnvelopeCountSummaryReportListResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.EnvelopeCountSummary;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.ReportsService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.ZipFileSummaryResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.util.CsvWriter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

@RestController
@RequestMapping(path = "/reports", produces = MediaType.APPLICATION_JSON_VALUE)
public class ReportsController {

    private final ReportsService reportsService;

    public ReportsController(ReportsService reportsService) {
        this.reportsService = reportsService;
    }

    @GetMapping(path = "/count-summary")
    @ApiOperation("Retrieves envelope count summary report")
    public EnvelopeCountSummaryReportListResponse getCountSummary(
        @RequestParam(name = "date") @DateTimeFormat(iso = DATE) LocalDate date,
        @RequestParam(name = "include-test", defaultValue = "false", required = false) boolean includeTestJurisdiction
    ) {
        List<EnvelopeCountSummary> result = this.reportsService.getCountFor(date, includeTestJurisdiction);
        return new EnvelopeCountSummaryReportListResponse(
            result
                .stream()
                .map(item -> new EnvelopeCountSummaryReportItem(
                    item.received,
                    item.rejected,
                    item.jurisdiction,
                    item.date
                ))
                .collect(toList())
        );
    }

    @GetMapping(path = "/zip-files-summary", produces = "application/csv")
    @ApiOperation("Retrieves zip files summary report for the given date and jurisdiction")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieves zip files summary in csv format.", response = byte[].class),
    })
    public ResponseEntity getZipFilesSummary(
        @RequestParam(name = "date") @DateTimeFormat(iso = DATE) LocalDate date,
        @RequestParam(name = "jurisdiction", required = false) String jurisdiction
    ) throws IOException {

        String fileName = String.format("Zipfiles-report-%s", date.toString());

        List<ZipFileSummaryResponse> result = this.reportsService.getZipFilesSummary(date, jurisdiction);

        File csvFile = CsvWriter.writeZipFilesSummaryToCsv(fileName, result);

        return ResponseEntity
            .ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(Files.readAllBytes(csvFile.toPath()));
    }

}
