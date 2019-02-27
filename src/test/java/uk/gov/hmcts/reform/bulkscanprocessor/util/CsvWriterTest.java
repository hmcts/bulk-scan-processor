package uk.gov.hmcts.reform.bulkscanprocessor.util;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.Test;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.ZipFileSummaryResponse;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.DOC_PROCESSED;

public class CsvWriterTest {

    @Test
    public void should_return_csv_file_with_headers_and_csv_records() throws IOException {
        LocalDate date = LocalDate.now();
        LocalTime time = LocalTime.now();

        //given
        List<ZipFileSummaryResponse> csvData = Arrays.asList(
            new ZipFileSummaryResponse(
                "test1.zip", date, time, date, time, "BULKSCAN", DOC_PROCESSED.toString()
            ),
            new ZipFileSummaryResponse(
                "test2.zip", date, time, date, time, "BULKSCAN", DOC_PROCESSED.toString()
            )
        );

        //when
        File summaryToCsv = CsvWriter.writeZipFilesSummaryToCsv("zipfilesummary.csv", csvData);

        //then
        List<CSVRecord> csvRecordList = readCsv(summaryToCsv);

        assertThat(csvRecordList)
            .isNotEmpty()
            .hasSize(3)
            .extracting(data -> tuple(
                data.get(0), data.get(1), data.get(2), data.get(3), data.get(4), data.get(5), data.get(6))
            )
            .containsExactly(
                tuple(
                    "Zip File Name",
                    "Date Received",
                    "Time Received",
                    "Date Processed",
                    "Time Processed",
                    "Jurisdiction",
                    "Status"
                ),
                tuple(
                    "test1.zip",
                    date.toString(),
                    time.toString(),
                    date.toString(),
                    time.toString(),
                    "BULKSCAN",
                    DOC_PROCESSED.toString()
                ),
                tuple(
                    "test2.zip",
                    date.toString(),
                    time.toString(),
                    date.toString(),
                    time.toString(),
                    "BULKSCAN",
                    DOC_PROCESSED.toString()
                )
            );
    }

    @Test
    public void should_return_csv_file_with_only_headers_when_the_data_is_null() throws IOException {
        //when
        File summaryToCsv = CsvWriter.writeZipFilesSummaryToCsv("zipfilesummary.csv", null);

        //then
        List<CSVRecord> csvRecordList = readCsv(summaryToCsv);

        assertThat(csvRecordList)
            .isNotEmpty()
            .hasSize(1)
            .extracting(data -> tuple(
                data.get(0), data.get(1), data.get(2), data.get(3), data.get(4), data.get(5), data.get(6))
            )
            .containsExactly(
                tuple(
                    "Zip File Name",
                    "Date Received",
                    "Time Received",
                    "Date Processed",
                    "Time Processed",
                    "Jurisdiction",
                    "Status"
                )
            );
    }

    private List<CSVRecord> readCsv(File summaryToCsv) throws IOException {
        Reader fileReader = new FileReader(summaryToCsv);
        CSVParser csvParser = CSVFormat.DEFAULT.parse(fileReader);

        return csvParser.getRecords();
    }
}
