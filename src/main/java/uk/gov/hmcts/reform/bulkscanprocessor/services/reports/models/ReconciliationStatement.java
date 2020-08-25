package uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.List;

public class ReconciliationStatement {
    public final LocalDate date;
    public final List<ReportedZipFile> envelopes;

    public ReconciliationStatement(
        @JsonProperty("date") LocalDate date,
        @JsonProperty("envelopes") List<ReportedZipFile> envelopes
    ) {
        this.date = date;
        this.envelopes = envelopes;
    }
}
