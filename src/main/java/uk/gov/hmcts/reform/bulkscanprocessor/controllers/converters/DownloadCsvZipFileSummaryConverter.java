package uk.gov.hmcts.reform.bulkscanprocessor.controllers.converters;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ReportException;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.ZipFileSummaryResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.util.CsvWriter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public final class DownloadCsvZipFileSummaryConverter {

    private DownloadCsvZipFileSummaryConverter() {
        // utility class constructor
    }

    public static ResponseEntity<byte[]> convert(List<ZipFileSummaryResponse> summary) {
        try {
            File csvFile = CsvWriter.writeZipFilesSummaryToCsv(summary);

            return ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=zip-files-summary.csv")
                .body(Files.readAllBytes(csvFile.toPath()));
        } catch (IOException exception) {
            throw new ReportException("Unable to generate response", exception);
        }
    }
}
