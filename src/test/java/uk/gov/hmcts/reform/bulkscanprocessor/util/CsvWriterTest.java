package uk.gov.hmcts.reform.bulkscanprocessor.util;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.ZipFileSummaryResponse;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification.SUPPLEMENTARY_EVIDENCE;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.DOC_UPLOADED;

class CsvWriterTest {

    @Test
    void should_return_csv_file_with_headers_and_csv_records() throws IOException {
        LocalDate date = LocalDate.now();
        LocalTime time = LocalTime.now();

        //given
        List<ZipFileSummaryResponse> csvData = Arrays.asList(
            new ZipFileSummaryResponse(
                "test1.zip",
                date,
                time,
                date,
                time,
                "bulkscan",
                DOC_UPLOADED.toString(),
                Status.UPLOADED.toString(),
                SUPPLEMENTARY_EVIDENCE.name(),
                "1321321312",
                "AUTO_CREATED_CASE"
            ),
            new ZipFileSummaryResponse(
                "test2.zip",
                date,
                time,
                date,
                time,
                "bulkscan",
                DOC_UPLOADED.toString(),
                Status.UPLOADED.toString(),
                null,
                null,
                null
            )
        );

        //when
        File summaryToCsv = CsvWriter.writeZipFilesSummaryToCsv(csvData);

        //then
        List<CSVRecord> csvRecordList = readCsv(summaryToCsv);

        assertThat(csvRecordList)
            .isNotEmpty()
            .hasSize(3)
            .extracting(data -> tuple(
                data.get(0),
                data.get(1),
                data.get(2),
                data.get(3),
                data.get(4),
                data.get(5),
                data.get(6),
                data.get(7),
                data.get(8),
                data.get(9))
            )
            .containsExactly(
                tuple(
                    "Container",
                    "Zip File Name",
                    "Date Received",
                    "Time Received",
                    "Date Processed",
                    "Time Processed",
                    "Status",
                    "Classification",
                    "CCD Action",
                    "CCD ID"
                ),
                tuple(
                    "bulkscan",
                    "test1.zip",
                    date.toString(),
                    time.toString(),
                    date.toString(),
                    time.toString(),
                    DOC_UPLOADED.toString(),
                    SUPPLEMENTARY_EVIDENCE.name(),
                    "AUTO_CREATED_CASE",
                    "1321321312"
                ),
                tuple(
                    "bulkscan",
                    "test2.zip",
                    date.toString(),
                    time.toString(),
                    date.toString(),
                    time.toString(),
                    DOC_UPLOADED.toString(),
                    "",
                    "",
                    ""
                )
            );
    }

    @Test
    void should_return_csv_file_with_only_headers_when_the_data_is_null() throws IOException {
        //when
        File summaryToCsv = CsvWriter.writeZipFilesSummaryToCsv(null);

        //then
        List<CSVRecord> csvRecordList = readCsv(summaryToCsv);

        assertThat(csvRecordList)
            .isNotEmpty()
            .hasSize(1)
            .extracting(data -> tuple(
                data.get(0),
                data.get(1),
                data.get(2),
                data.get(3),
                data.get(4),
                data.get(5),
                data.get(6),
                data.get(7),
                data.get(8),
                data.get(9))
            )
            .containsExactly(
                tuple(
                    "Container",
                    "Zip File Name",
                    "Date Received",
                    "Time Received",
                    "Date Processed",
                    "Time Processed",
                    "Status",
                    "Classification",
                    "CCD Action",
                    "CCD ID"
                )
            );
    }

    private List<CSVRecord> readCsv(File summaryToCsv) throws IOException {
        return CSVFormat.DEFAULT.parse(new FileReader(summaryToCsv)).getRecords();
    }
}
