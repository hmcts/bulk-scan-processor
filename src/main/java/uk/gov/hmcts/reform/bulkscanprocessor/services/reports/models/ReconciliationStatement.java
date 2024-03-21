package uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.List;

/**
 * Represents the reconciliation statement.
 */
public class ReconciliationStatement {
    public final LocalDate date;
    public final List<ReportedZipFile> envelopes;

    /**
     * Constructor for the ReconciliationStatement.
     * @param date The date
     * @param envelopes The envelopes
     */
    public ReconciliationStatement(
        @JsonProperty("date") LocalDate date,
        @JsonProperty("envelopes") List<ReportedZipFile> envelopes
    ) {
        this.date = date;
        this.envelopes = envelopes;
    }
}
