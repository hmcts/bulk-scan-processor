package uk.gov.hmcts.reform.bulkscanprocessor.util;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.ZipFileSummaryResponse;

import java.io.IOException;
import java.util.List;

public final class CsvWriter {

    private static final String[] ZIP_FILES_SUMMARY_CSV_HEADERS = {
        "Zip File Name", "Date Received", "Time Received", "Date Processed", "Time Processed", "Jurisdiction", "Status"
    };

    private CsvWriter() {
        // utility class constructor
    }

    public static String writeZipFilesSummaryToCsv(List<ZipFileSummaryResponse> data) throws IOException {

        CSVFormat csvFileHeader = CSVFormat.DEFAULT.withHeader(ZIP_FILES_SUMMARY_CSV_HEADERS);
        StringBuilder stringBuilder = new StringBuilder();

        try (CSVPrinter printer = new CSVPrinter(stringBuilder, csvFileHeader)) {
            for (ZipFileSummaryResponse summary : CollectionUtils.emptyIfNull(data)) {
                printer.printRecord(
                    summary.fileName,
                    summary.dateReceived,
                    summary.timeReceived,
                    summary.dateProcessed,
                    summary.timeProcessed,
                    summary.jurisdiction,
                    summary.status
                );
            }
        }
        return stringBuilder.toString();
    }
}
