package uk.gov.hmcts.reform.bulkscanprocessor.model.out.reports;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.LocalTime;

public class ZipFilesSummaryReportItem {

    @JsonProperty("file_name")
    public final String fileName;

    @JsonProperty("date_received")
    public final LocalDate dateReceived;

    @JsonProperty("time_received")
    @JsonFormat(pattern = "HH:mm:ss")
    public final LocalTime timeReceived;

    @JsonProperty("date_processed")
    public final LocalDate dateProcessed;

    @JsonProperty("time_processed")
    @JsonFormat(pattern = "HH:mm:ss")
    public final LocalTime timeProcessed;

    @JsonProperty("container")
    public final String jurisdiction;

    @JsonProperty("status")
    public final String status;

    // region constructor
    public ZipFilesSummaryReportItem(String fileName,
                                     LocalDate dateReceived,
                                     LocalTime timeReceived,
                                     LocalDate dateProcessed,
                                     LocalTime timeProcessed,
                                     String container,
                                     String status
    ) {
        this.fileName = fileName;
        this.dateReceived = dateReceived;
        this.timeReceived = timeReceived;
        this.dateProcessed = dateProcessed;
        this.timeProcessed = timeProcessed;
        this.jurisdiction = container;
        this.status = status;
    }
    // endregion

}
