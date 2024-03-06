package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import io.swagger.v3.oas.annotations.Operation;
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
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.ReceivedPayment;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.ReceivedScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.ReceivedScannableItemPerDocumentType;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.RejectedZipFile;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.reports.DiscrepancyItem;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.reports.EnvelopeCountSummaryReportItem;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.reports.EnvelopeCountSummaryReportListResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.reports.ReceivedPaymentsResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.reports.ReceivedScannableItemsResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.reports.ReconciliationReportResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.reports.RejectedFilesResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.reports.RejectedZipFilesResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.reports.ZipFilesSummaryReportItem;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.reports.ZipFilesSummaryReportListResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.ReceivedPaymentsService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.ReceivedScannableItemsService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.ReconciliationService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.RejectedFilesReportService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.RejectedZipFilesService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.ReportsService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.Discrepancy;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.EnvelopeCountSummary;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.ReceivedPaymentsData;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.ReceivedScannableItemsData;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.ReceivedScannableItemsPerDocumentTypeData;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.ReconciliationStatement;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.RejectedFile;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.RejectedZipFileData;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.ZipFileSummaryResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.util.CsvWriter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import jakarta.validation.ClockProvider;

import static java.util.stream.Collectors.toList;
import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

@RestController
@CrossOrigin
@RequestMapping(path = "/reports")
public class ReportsController {

    private final ReportsService reportsService;
    private final RejectedFilesReportService rejectedFilesReportService;
    private final RejectedZipFilesService rejectedZipFilesService;
    private final ReconciliationService reconciliationService;
    private final ReceivedScannableItemsService receivedScannableItemsService;
    private final ReceivedPaymentsService receivedPaymentsService;
    private final ClockProvider clockProvider;

    // region constructor
    public ReportsController(
            ReportsService reportsService,
            RejectedFilesReportService rejectedFilesReportService,
            RejectedZipFilesService rejectedZipFilesService,
            ReconciliationService reconciliationService,
            ReceivedScannableItemsService receivedScannableItemsService,
            ReceivedPaymentsService receivedPaymentsService,
            ClockProvider clockProvider
    ) {
        this.reportsService = reportsService;
        this.rejectedFilesReportService = rejectedFilesReportService;
        this.rejectedZipFilesService = rejectedZipFilesService;
        this.reconciliationService = reconciliationService;
        this.receivedScannableItemsService = receivedScannableItemsService;
        this.receivedPaymentsService = receivedPaymentsService;
        this.clockProvider = clockProvider;
    }
    // endregion

    @GetMapping(path = "/count-summary", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(description = "Retrieves envelope count summary report")
    public EnvelopeCountSummaryReportListResponse getCountSummary(
        @RequestParam(name = "date") @DateTimeFormat(iso = DATE) LocalDate date,
        @RequestParam(name = "include-test", defaultValue = "false", required = false) boolean includeTestContainer
    ) {
        List<EnvelopeCountSummary> result = this.reportsService.getCountFor(date, includeTestContainer);
        return getEnvelopeCountSummaryReportListResponse(result);
    }

    @GetMapping(path = "/envelopes-count-summary", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(description = "Retrieves count summary report for envelopes")
    public EnvelopeCountSummaryReportListResponse getEnvelopeCountSummary(
        @RequestParam(name = "date") @DateTimeFormat(iso = DATE) LocalDate date,
        @RequestParam(name = "include-test", defaultValue = "false", required = false) boolean includeTestContainer
    ) {
        List<EnvelopeCountSummary> result = reportsService.getEnvelopeSummaryCountFor(date, includeTestContainer);
        return getEnvelopeCountSummaryReportListResponse(result);
    }

    @GetMapping(path = "/count-summary-report", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(description = "Retrieves envelope count summary report")
    public EnvelopeCountSummaryReportListResponse getSummaryCountFor(
        @RequestParam(name = "date") @DateTimeFormat(iso = DATE) LocalDate date,
        @RequestParam(name = "include-test", defaultValue = "false", required = false) boolean includeTestContainer
    ) {
        List<EnvelopeCountSummary> result = this.reportsService.getSummaryCountFor(date, includeTestContainer);
        return getEnvelopeCountSummaryReportListResponse(result);
    }

    @GetMapping(path = "/zip-files-summary", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(description = "Retrieves zip files summary report in json format for the given date and container")
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
    @Operation(description = "Retrieves zip files summary report in csv format for the given date and container")
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
    @Operation(description = "Retrieves rejected files")
    public RejectedFilesResponse getRejectedFiles() {
        List<RejectedFile> rejectedEnvs = rejectedFilesReportService.getRejectedFiles();
        return new RejectedFilesResponse(rejectedEnvs.size(), rejectedEnvs);
    }

    @GetMapping(path = "/rejected-zip-files", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(description = "Retrieves rejected files")
    public RejectedZipFilesResponse getRejectedZipFiles(
            @RequestParam(name = "date") @DateTimeFormat(iso = DATE) LocalDate date
    ) {
        List<RejectedZipFile> result = rejectedZipFilesService.getRejectedZipFiles(date);
        return new RejectedZipFilesResponse(
                result.size(),
                result
                        .stream()
                        .map(file -> new RejectedZipFileData(
                                file.getZipFileName(),
                                file.getContainer(),
                                LocalDateTime.ofInstant(file.getProcessingStartedEventDate(), ZoneId.of("UTC")),
                                file.getEnvelopeId(),
                                file.getEvent()
                        ))
                        .collect(toList())
        );
    }

    @PostMapping(
        path = "/reconciliation",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(description = "Retrieves reconciliation report")
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

    @GetMapping(path = "/received-scannable-items", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(description = "Retrieves scannable items")
    public ReceivedScannableItemsResponse getReceivedScannableItems(
            @RequestParam(name = "date") @DateTimeFormat(iso = DATE) LocalDate date,
            @RequestParam(name = "per_document_type", required = false) boolean perDocumentType
    ) {
        AtomicInteger total = new AtomicInteger(0);
        List<ReceivedScannableItemsData> receivedScannableItems;

        if (perDocumentType) {
            List<ReceivedScannableItemPerDocumentType> result =
                    receivedScannableItemsService.getReceivedScannableItemsPerDocumentType(date);
            receivedScannableItems = result
                    .stream()
                    .map(scannableItem -> {
                                total.addAndGet(scannableItem.getCount());
                                return new ReceivedScannableItemsPerDocumentTypeData(
                                        scannableItem.getContainer(),
                                        scannableItem.getDocumentType(),
                                        scannableItem.getCount()
                                );
                            }
                    )
                    .collect(toList());
        } else {
            List<ReceivedScannableItem> result = receivedScannableItemsService.getReceivedScannableItems(date);
            receivedScannableItems = result
                    .stream()
                    .map(scannableItem -> {
                                total.addAndGet(scannableItem.getCount());
                                return new ReceivedScannableItemsData(
                                        scannableItem.getContainer(),
                                        scannableItem.getCount()
                                );
                            }
                    )
                    .collect(toList());
        }

        return new ReceivedScannableItemsResponse(
                total.get(),
                receivedScannableItems
        );
    }

    @GetMapping(path = "/received-payments", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(description = "Retrieves payments")
    public ReceivedPaymentsResponse getReceivedPayments(
            @RequestParam(name = "date") @DateTimeFormat(iso = DATE) LocalDate date,
            @RequestParam(name = "per_document_type", required = false) boolean perDocumentType
    ) {
        AtomicInteger total = new AtomicInteger(0);

        List<ReceivedPayment> result = receivedPaymentsService.getReceivedPayments(date);
        List<ReceivedPaymentsData> receivedPayments = result
                .stream()
                .map(scannableItem -> {
                            total.addAndGet(scannableItem.getCount());
                            return new ReceivedPaymentsData(
                                    scannableItem.getContainer(),
                                    scannableItem.getCount()
                            );
                        }
                )
                .collect(toList());

        return new ReceivedPaymentsResponse(
                total.get(),
                receivedPayments
        );
    }

    private EnvelopeCountSummaryReportListResponse getEnvelopeCountSummaryReportListResponse(
            List<EnvelopeCountSummary> result
    ) {
        return new EnvelopeCountSummaryReportListResponse(
            result
                .stream()
                .map(item -> new EnvelopeCountSummaryReportItem(
                    item.received,
                    item.rejected,
                    item.container,
                    item.date
                ))
                .collect(toList()),
            clockProvider
        );
    }
}
