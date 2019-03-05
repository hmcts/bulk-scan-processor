package uk.gov.hmcts.reform.bulkscanprocessor.controllers.converters;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.reports.ZipFilesSummaryReportItem;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.reports.ZipFilesSummaryReportListResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.ZipFileSummaryResponse;

import java.util.List;

import static java.util.stream.Collectors.toList;

public final class JsonViewZipFileSummaryConverter {

    private JsonViewZipFileSummaryConverter() {
        // utility class constructor
    }

    public static ResponseEntity<ZipFilesSummaryReportListResponse> convert(List<ZipFileSummaryResponse> summary) {
        ZipFilesSummaryReportListResponse response = new ZipFilesSummaryReportListResponse(
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

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .body(response);
    }
}
