package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import io.swagger.annotations.ApiOperation;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.reports.DiscrepancyItem;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.reports.EnvelopeCountSummaryReportItem;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.reports.EnvelopeCountSummaryReportListResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.reports.ReconciliationReportResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.reports.RejectedFilesResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.reports.ZipFilesSummaryReportItem;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.reports.ZipFilesSummaryReportListResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.ReconciliationService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.RejectedFilesReportService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.ReportsService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.Discrepancy;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.EnvelopeCountSummary;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.ReconciliationStatement;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.RejectedFile;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.ZipFileSummaryResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.util.CsvWriter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

@RestController
@CrossOrigin
@RequestMapping(path = "/reports")
public class ReportsController {

    private final ReportsService reportsService;
    private final RejectedFilesReportService rejectedFilesReportService;
    private final ReconciliationService reconciliationService;

    // region constructor
    public ReportsController(
        ReportsService reportsService,
        RejectedFilesReportService rejectedFilesReportService,
        ReconciliationService reconciliationService
    ) {
        this.reportsService = reportsService;
        this.rejectedFilesReportService = rejectedFilesReportService;
        this.reconciliationService = reconciliationService;
    }
    // endregion

    @GetMapping(path = "/count-summary", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation("Retrieves envelope count summary report")
    public EnvelopeCountSummaryReportListResponse getCountSummary(
        @RequestParam(name = "date") @DateTimeFormat(iso = DATE) LocalDate date,
        @RequestParam(name = "include-test", defaultValue = "false", required = false) boolean includeTestContainer
    ) {
        List<EnvelopeCountSummary> result = this.reportsService.getCountFor(date, includeTestContainer);
        return new EnvelopeCountSummaryReportListResponse(result
                                                          .stream()
                                                          .map(item -> new EnvelopeCountSummaryReportItem(
                                                              item.received,
                                                              item.rejected,
                                                              item.container,
                                                              item.date
                                                          ))
                                                          .collect(toList()));
    }

    @GetMapping(path = "/zip-files-summary", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation("Retrieves zip files summary report in json format for the given date and container")
    public ZipFilesSummaryReportListResponse getZipFilesSummary(
        @RequestParam(name = "date") @DateTimeFormat(iso = DATE) LocalDate date,
        @RequestParam(name = "container", required = false) String container,
        @RequestParam(name = "classification", required = false) Classification classification
    ) {
        List<ZipFileSummaryResponse> summary = reportsService.getZipFilesSummary(date, container, classification);
        return new ZipFilesSummaryReportListResponse(
            summary
                .stream()
                .map(item -> new ZipFilesSummaryReportItem(
                    item.fileName,
                    item.dateReceived,
                    item.timeReceived,
                    item.dateProcessed,
                    item.timeProcessed,
                    item.container,
                    item.lastEventStatus,
                    item.envelopeStatus,
                    item.classification,
                    item.ccdId,
                    item.ccdAction
                ))
                .collect(toList())
        );
    }

    @GetMapping(path = "/zip-files-summary", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ApiOperation("Retrieves zip files summary report in csv format for the given date and container")
    public ResponseEntity downloadZipFilesSummary(
        @RequestParam(name = "date") @DateTimeFormat(iso = DATE) LocalDate date,
        @RequestParam(name = "container", required = false) String container
    ) throws IOException {
        List<ZipFileSummaryResponse> summary = this.reportsService.getZipFilesSummary(date, container, null);

        File csvFile = CsvWriter.writeZipFilesSummaryToCsv(summary);
        return ResponseEntity
            .ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=zip-files-summary.csv")
            .body(Files.readAllBytes(csvFile.toPath()));
    }

    @GetMapping(path = "/rejected", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation("Retrieves rejected files")
    public RejectedFilesResponse getRejectedFiles() {
        List<RejectedFile> rejectedEnvs = rejectedFilesReportService.getRejectedFiles();
        return new RejectedFilesResponse(rejectedEnvs.size(), rejectedEnvs);
    }

    @PostMapping(
        path = "/reconciliation",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ApiOperation("Retrieves reconciliation report")
    public ReconciliationReportResponse getReconciliationReport(
        @RequestBody ReconciliationStatement statement
    ) {
        List<Discrepancy> result = reconciliationService.getReconciliationReport(statement);
        return new ReconciliationReportResponse(
            result
                .stream()
                .map(item -> new DiscrepancyItem(
                    item.zipFileName,
                    item.container,
                    item.type,
                    item.stated,
                    item.actual
                ))
                .collect(toList())
        );
    }
}
