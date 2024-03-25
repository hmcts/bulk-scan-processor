package uk.gov.hmcts.reform.bulkscanprocessor.util;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.ZipFileSummaryResponse;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

/**
 * Utility class to write CSV files.
 */
public final class CsvWriter {

    private static final String[] ZIP_FILES_SUMMARY_CSV_HEADERS = {
        "Container", "Zip File Name", "Date Received", "Time Received",
        "Date Processed", "Time Processed", "Status", "Classification", "CCD Action", "CCD ID"
    };

    /**
     * Constructor to CsvWriter.
     */
    private CsvWriter() {
        // utility class constructor
    }

    /**
     * Writes the given data to a CSV file.
     *
     * @param data list of ZipFileSummaryResponse
     * @return the CSV file
     * @throws IOException if there is an error writing the file
     */
    public static File writeZipFilesSummaryToCsv(
        List<ZipFileSummaryResponse> data
    ) throws IOException {
        File csvFile = File.createTempFile("Zipfiles-summary-", ".csv");

        CSVFormat csvFileHeader = CSVFormat.DEFAULT.withHeader(ZIP_FILES_SUMMARY_CSV_HEADERS);
        try (
            FileWriter fileWriter = new FileWriter(csvFile);
            CSVPrinter printer = new CSVPrinter(fileWriter, csvFileHeader)
        ) {
            for (ZipFileSummaryResponse summary : ofNullable(data).orElse(emptyList())) {
                printer.printRecord(
                    summary.container,
                    summary.fileName,
                    summary.dateReceived,
                    summary.timeReceived,
                    summary.dateProcessed,
                    summary.timeProcessed,
                    summary.lastEventStatus,
                    summary.classification,
                    summary.ccdAction,
                    summary.ccdId
                );
            }
        }
        return csvFile;
    }
}
