package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import io.swagger.annotations.ApiOperation;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.reports.EnvelopeCountSummaryReportItem;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.reports.EnvelopeCountSummaryReportListResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.reports.ZipFilesSummaryReportItem;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.reports.ZipFilesSummaryReportListResponse;
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
@RequestMapping(path = "/reports")
public class ReportsController {

    private final ReportsService reportsService;

    public ReportsController(ReportsService reportsService) {
        this.reportsService = reportsService;
    }

    @GetMapping(path = "/count-summary", produces = MediaType.APPLICATION_JSON_VALUE)
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

    @GetMapping(path = "/zip-files-summary", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation("Retrieves zip files summary report in json format for the given date and jurisdiction")
    public ZipFilesSummaryReportListResponse getZipFilesSummary(
        @RequestParam(name = "date") @DateTimeFormat(iso = DATE) LocalDate date,
        @RequestParam(name = "jurisdiction", required = false) String jurisdiction
    ) {
        List<ZipFileSummaryResponse> summary = this.reportsService.getZipFilesSummary(date, jurisdiction);
        return new ZipFilesSummaryReportListResponse(
            summary
                .stream()
                .map(item -> new ZipFilesSummaryReportItem(
                    item.fileName,
                    item.dateReceived,
                    item.timeReceived,
                    item.dateProcessed,
                    item.timeProcessed,
                    item.jurisdiction,
                    item.status
                ))
                .collect(toList())
        );
    }

    @GetMapping(path = "/download-zip-files-summary", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ApiOperation("Retrieves zip files summary report in csv format for the given date and jurisdiction")
    public ResponseEntity downloadZipFilesSummary(
        @RequestParam(name = "date") @DateTimeFormat(iso = DATE) LocalDate date,
        @RequestParam(name = "jurisdiction", required = false) String jurisdiction
    ) throws IOException {
        List<ZipFileSummaryResponse> summary = this.reportsService.getZipFilesSummary(date, jurisdiction);

        String fileName = String.format("Zipfiles-summary-%s", date.toString());
        File csvFile = CsvWriter.writeZipFilesSummaryToCsv(fileName, summary);

        return ResponseEntity
            .ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(Files.readAllBytes(csvFile.toPath()));
    }

}
