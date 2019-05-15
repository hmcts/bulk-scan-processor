package uk.gov.hmcts.reform.bulkscanprocessor.util;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.ZipFileSummaryResponse;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public final class CsvWriter {

    private static final String[] ZIP_FILES_SUMMARY_CSV_HEADERS = {
        "Container", "Zip File Name", "Date Received", "Time Received", "Date Processed", "Time Processed", "Status"
    };

    private CsvWriter() {
        // utility class constructor
    }

    public static File writeZipFilesSummaryToCsv(
        List<ZipFileSummaryResponse> data
    ) throws IOException {
        File csvFile = File.createTempFile("Zipfiles-summary-", ".csv");

        CSVFormat csvFileHeader = CSVFormat.DEFAULT.withHeader(ZIP_FILES_SUMMARY_CSV_HEADERS);
        FileWriter fileWriter = new FileWriter(csvFile);

        try (CSVPrinter printer = new CSVPrinter(fileWriter, csvFileHeader)) {
            for (ZipFileSummaryResponse summary : CollectionUtils.emptyIfNull(data)) {
                printer.printRecord(
                    summary.container,
                    summary.fileName,
                    summary.dateReceived,
                    summary.timeReceived,
                    summary.dateProcessed,
                    summary.timeProcessed,
                    summary.status
                );
            }
        }
        return csvFile;
    }
}
